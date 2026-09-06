import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';

const session = {
  accessToken: 'new-access-token',
  user: {
    id: 'user-id',
    username: 'alice',
    name: 'Alice',
    email: 'alice@example.com',
  },
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  const router = { navigate: vi.fn().mockResolvedValue(true) };

  const completeInitialization = async (): Promise<void> => {
    const initialization = service.initialize();
    http.expectOne('/api/auth/session').flush({ hasRefreshCookie: true });
    await new Promise<void>((resolve) => setTimeout(resolve));
    http.expectOne('/api/auth/refresh').flush(session);
    await initialization;
  };

  beforeEach(() => {
    localStorage.setItem('auth', JSON.stringify({ accessToken: 'legacy' }));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });

    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('restores the cookie-backed session during initialization without persisting it', async () => {
    await completeInitialization();

    expect(service.isLoading()).toBe(false);
    expect(service.isAuthenticated()).toBe(true);
    expect(service.user()).toEqual(session.user);
    expect(service.getToken()).toBe(session.accessToken);
    expect(localStorage.getItem('auth')).toBeNull();
  });

  it('does not call refresh during anonymous initialization', async () => {
    const initialization = service.initialize();
    http.expectOne('/api/auth/session').flush({ hasRefreshCookie: false });

    await initialization;

    http.expectNone('/api/auth/refresh');
    expect(service.isLoading()).toBe(false);
    expect(service.isAuthenticated()).toBe(false);
  });

  it('shares concurrent refresh calls', async () => {
    await completeInitialization();

    const first = service.refreshToken();
    const second = service.refreshToken();
    const refreshRequest = http.expectOne('/api/auth/refresh');
    refreshRequest.flush({ ...session, accessToken: 'replacement-token' });

    await expect(first).resolves.toBe('replacement-token');
    await expect(second).resolves.toBe('replacement-token');
  });

  it('clears local state and navigates to login when refresh fails', async () => {
    await completeInitialization();

    const refresh = service.refreshToken();
    http.expectOne('/api/auth/refresh').flush('expired', {
      status: 401,
      statusText: 'Unauthorized',
    });
    await expect(refresh).resolves.toBeNull();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
    expect(service.getToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('clears local state and navigates after a failed server logout', async () => {
    await completeInitialization();

    const logout = service.logout();
    http.expectOne('/api/auth/logout').flush('expired', {
      status: 401,
      statusText: 'Unauthorized',
    });
    await logout;

    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
    expect(service.getToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('immediately clears local state and ignores an in-flight refresh after logout', async () => {
    await completeInitialization();

    const refresh = service.refreshToken();
    const refreshRequest = http.expectOne('/api/auth/refresh');
    const logout = service.logout();
    const logoutRequest = http.expectOne('/api/auth/logout');

    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
    expect(service.getToken()).toBeNull();

    logoutRequest.flush(null);
    await logout;
    refreshRequest.flush({ ...session, accessToken: 'stale-token' });
    await expect(refresh).resolves.toBeNull();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
    expect(service.getToken()).toBeNull();
  });
});
