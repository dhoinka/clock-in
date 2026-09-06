import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom, map, of, catchError } from 'rxjs';
import {
  AuthRequest,
  SignUpRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  User,
  Auth,
  authSchema,
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly _user = signal<User | null>(null);
  private readonly _isAuthenticated = signal<boolean>(false);
  private readonly _isLoading = signal<boolean>(true);
  private accessToken: string | null = null;
  private refreshInFlight: Promise<string | null> | null = null;
  private sessionGeneration = 0;
  private initialization: Promise<void> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();

  constructor() {
    // Remove the legacy browser-persisted session if a user upgrades in place.
    localStorage.removeItem('auth');
  }

  async initialize(): Promise<void> {
    if (!this.initialization) {
      this.initialization = this.restoreSession();
    }
    await this.initialization;
  }

  private async restoreSession(): Promise<void> {
    try {
      if (await this.hasRefreshCookie()) {
        await this.refreshToken();
      } else {
        this.clearSession();
      }
    } catch {
      this.clearSession();
    } finally {
      this._isLoading.set(false);
    }
  }

  private async hasRefreshCookie(): Promise<boolean> {
    return firstValueFrom(
      this.http
        .get<{ hasRefreshCookie: boolean }>('/api/auth/session', {
          withCredentials: true,
        })
        .pipe(
          map((response) => response.hasRefreshCookie),
          catchError(() => of(false)),
        ),
    );
  }

  async login(credentials: AuthRequest): Promise<void> {
    const response = await firstValueFrom(
      this.http
        .post(
          '/api/auth/login',
          {
            ...credentials,
          },
          { withCredentials: true },
        )
        .pipe(map((res) => authSchema.parse(res))),
    );
    this.setSession(response);
    await this.router.navigate(['/home']);
  }

  async signUp(data: SignUpRequest): Promise<User> {
    const response = await firstValueFrom(
      this.http
        .post(
          '/api/auth/signup',
          {
            ...data,
          },
          { withCredentials: true },
        )
        .pipe(map((res) => authSchema.parse(res))),
    );
    this.setSession(response);
    return response.user;
  }

  async logout(): Promise<void> {
    this.sessionGeneration += 1;
    this.clearSession();

    try {
      await firstValueFrom(
        this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }),
      );
    } catch {
      // Local sign-out must still succeed if the server session has expired.
    } finally {
      await this.router.navigate(['/login']);
    }
  }

  private clearSession(): void {
    this.accessToken = null;
    this._user.set(null);
    this._isAuthenticated.set(false);
  }

  async forgotPassword(data: ForgotPasswordRequest): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/forgot-password', data));
  }

  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/reset-password', data));
  }

  async refreshToken(): Promise<string | null> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }

    const generation = this.sessionGeneration;
    const refresh = this.requestRefresh(generation);
    this.refreshInFlight = refresh;
    try {
      return await refresh;
    } finally {
      if (this.refreshInFlight === refresh) {
        this.refreshInFlight = null;
      }
    }
  }

  private async requestRefresh(generation: number): Promise<string | null> {
    try {
      const response = await firstValueFrom(
        this.http
          .post('/api/auth/refresh', {}, { withCredentials: true })
          .pipe(map((res) => authSchema.parse(res))),
      );
      if (generation !== this.sessionGeneration) {
        return null;
      }
      this.setSession(response);
      return response.accessToken;
    } catch (error: unknown) {
      if (generation !== this.sessionGeneration) {
        return null;
      }
      console.warn(error);
      this.clearSession();
      await this.router.navigate(['/login']);
      return null;
    }
  }

  private setSession(response: Auth): void {
    this.accessToken = response.accessToken;
    this._user.set(response.user);
    this._isAuthenticated.set(true);
  }

  updateUser(userData: Partial<User>): void {
    this._user.update((prev) => {
      if (!prev) return null;
      return { ...prev, ...userData };
    });
  }

  getToken(): string | null {
    return this.accessToken;
  }
}
