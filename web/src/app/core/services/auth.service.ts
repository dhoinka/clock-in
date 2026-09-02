import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';
import {
  AuthRequest,
  SignUpRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  User,
  authSchema,
  userSchema,
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly _user = signal<User | null>(null);
  private readonly _isAuthenticated = signal<boolean>(false);
  private readonly _isLoading = signal<boolean>(true);

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();

  constructor() {
    this.checkAuth();
  }

  private checkAuth(): void {
    try {
      const authData = localStorage.getItem('auth');
      if (authData) {
        const auth = authSchema.parse(JSON.parse(authData));
        if (auth?.accessToken) {
          this._user.set(auth.user);
          this._isAuthenticated.set(true);
        }
      }
    } catch {
      localStorage.removeItem('auth');
    } finally {
      this._isLoading.set(false);
    }
  }

  async login(credentials: AuthRequest): Promise<void> {
    const response = await firstValueFrom(
      this.http
        .post('/api/auth/login', {
          ...credentials,
        })
        .pipe(map((res) => authSchema.parse(res))),
    );
    localStorage.setItem('auth', JSON.stringify(response));
    this._user.set(response.user);
    this._isAuthenticated.set(true);
    await this.router.navigate(['/home']);
  }

  async signUp(data: SignUpRequest): Promise<User> {
    return firstValueFrom(
      this.http
        .post('/api/auth/signup', {
          ...data,
        })
        .pipe(map((res) => userSchema.parse(res))),
    );
  }

  logout(): void {
    localStorage.removeItem('auth');
    this._user.set(null);
    this._isAuthenticated.set(false);
    this.router.navigate(['/login']);
  }

  async forgotPassword(data: ForgotPasswordRequest): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/forgot-password', data));
  }

  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/reset-password', data));
  }

  async refreshToken(): Promise<string | null> {
    const authData = localStorage.getItem('auth');
    if (!authData) {
      this.logout();
      return null;
    }

    const auth = authSchema.parse(JSON.parse(authData));
    try {
      const response = await firstValueFrom(
        this.http
          .post('/api/auth/refresh', {
            refreshToken: auth.refreshToken,
          })
          .pipe(map((res) => authSchema.parse(res))),
      );
      localStorage.setItem('auth', JSON.stringify(response));
      this._user.set(response.user);
      this._isAuthenticated.set(true);
      return response.accessToken;
    } catch {
      this.logout();
      return null;
    }
  }

  updateUser(userData: Partial<User>): void {
    this._user.update((prev) => {
      if (!prev) return null;
      const updated = { ...prev, ...userData };
      const authData = localStorage.getItem('auth');
      if (authData) {
        const auth = authSchema.parse(JSON.parse(authData));
        localStorage.setItem(
          'auth',
          JSON.stringify({ ...auth, user: updated }),
        );
      }
      return updated;
    });
  }

  getToken(): string | null {
    const authData = localStorage.getItem('auth');
    if (!authData) return null;
    const auth = authSchema.parse(JSON.parse(authData));
    return auth?.accessToken ?? null;
  }

  getRefreshToken(): string | null {
    const authData = localStorage.getItem('auth');
    if (!authData) return null;
    const auth = authSchema.parse(JSON.parse(authData));
    return auth?.refreshToken ?? null;
  }
}
