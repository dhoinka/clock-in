import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let authService: {
    getToken: ReturnType<typeof vi.fn>;
    refreshToken: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    authService = {
      getToken: vi.fn().mockReturnValue('expired-token'),
      refreshToken: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
      ],
    });

    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('does not refresh after a 403 response', () => {
    client.get('/api/worklogs').subscribe({ error: () => undefined });

    const request = http.expectOne('/api/worklogs');
    expect(request.request.headers.get('Authorization')).toBe(
      'Bearer expired-token',
    );
    request.flush('forbidden', { status: 403, statusText: 'Forbidden' });

    expect(authService.refreshToken).not.toHaveBeenCalled();
  });

  it('retries concurrent 401 requests with the replacement token', async () => {
    let resolveRefresh!: (token: string | null) => void;
    const refresh = new Promise<string | null>((resolve) => {
      resolveRefresh = resolve;
    });
    authService.refreshToken.mockReturnValue(refresh);

    const firstResponse = firstValueFrom(
      client.get<{ request: string }>('/api/worklogs/one'),
    );
    const secondResponse = firstValueFrom(
      client.get<{ request: string }>('/api/worklogs/two'),
    );

    const initialRequests = http.match((request) =>
      request.url.startsWith('/api/worklogs/'),
    );
    expect(initialRequests).toHaveLength(2);
    for (const request of initialRequests) {
      expect(request.request.headers.get('Authorization')).toBe(
        'Bearer expired-token',
      );
      request.flush('expired', { status: 401, statusText: 'Unauthorized' });
    }

    expect(authService.refreshToken).toHaveBeenCalledTimes(2);
    resolveRefresh('replacement-token');
    await new Promise<void>((resolve) => setTimeout(resolve));

    const retries = http.match((request) =>
      request.url.startsWith('/api/worklogs/'),
    );
    expect(retries).toHaveLength(2);
    for (const request of retries) {
      expect(request.request.headers.get('Authorization')).toBe(
        'Bearer replacement-token',
      );
      request.flush({ request: request.request.url.split('/').at(-1) });
    }

    await expect(firstResponse).resolves.toEqual({ request: 'one' });
    await expect(secondResponse).resolves.toEqual({ request: 'two' });
  });

  it('does not add bearer tokens or attempt a refresh for auth endpoints', () => {
    client.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    client.post('/api/auth/refresh', {}).subscribe({ error: () => undefined });
    client.post('/api/auth/logout', {}).subscribe({ error: () => undefined });
    client.get('/api/auth/session').subscribe({ error: () => undefined });

    const authRequests = http.match((request) =>
      request.url.startsWith('/api/auth/'),
    );
    expect(authRequests).toHaveLength(4);
    for (const request of authRequests) {
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush('unauthorized', {
        status: 401,
        statusText: 'Unauthorized',
      });
    }

    expect(authService.refreshToken).not.toHaveBeenCalled();
  });
});
