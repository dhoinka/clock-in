import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
  HttpContextToken,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const RETRIED_AFTER_REFRESH = new HttpContextToken<boolean>(() => false);

const isAuthEndpoint = (url: string): boolean =>
  /\/api\/auth\/(login|signup|refresh|logout|session)(?:[/?#]|$)/.test(url);

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  let authReq = req;
  if (token && !isAuthEndpoint(req.url)) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status === 401 &&
        !isAuthEndpoint(req.url) &&
        !req.context.get(RETRIED_AFTER_REFRESH)
      ) {
        const currentToken = authService.getToken();
        if (token && currentToken && currentToken !== token) {
          return next(
            req.clone({
              context: req.context.set(RETRIED_AFTER_REFRESH, true),
              setHeaders: { Authorization: `Bearer ${currentToken}` },
            }),
          );
        }
        return from(authService.refreshToken()).pipe(
          switchMap((newToken) => {
            if (newToken) {
              const retryReq = req.clone({
                context: req.context.set(RETRIED_AFTER_REFRESH, true),
                setHeaders: { Authorization: `Bearer ${newToken}` },
              });
              return next(retryReq);
            }
            return throwError(() => error);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
