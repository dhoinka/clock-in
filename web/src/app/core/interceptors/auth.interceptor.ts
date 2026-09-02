import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { from } from 'rxjs';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  let authReq = req;
  if (
    token &&
    !req.url.includes('/auth/login') &&
    !req.url.includes('/auth/refresh')
  ) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        (error.status === 401 || error.status === 403) &&
        !req.url.includes('/auth/login') &&
        !req.url.includes('/auth/refresh')
      ) {
        if (!isRefreshing) {
          isRefreshing = true;
          return from(authService.refreshToken()).pipe(
            switchMap((newToken) => {
              isRefreshing = false;
              if (newToken) {
                const retryReq = req.clone({
                  setHeaders: { Authorization: `Bearer ${newToken}` },
                });
                return next(retryReq);
              }
              return throwError(() => error);
            }),
            catchError((err) => {
              isRefreshing = false;
              return throwError(() => err);
            }),
          );
        }
      }
      return throwError(() => error);
    }),
  );
};
