import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse, HttpEvent, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { BehaviorSubject, catchError, filter, finalize, switchMap, take, throwError, Observable} from 'rxjs';

import { LoaderService } from '../ui/loader/loader.service';
import { ToastService } from '../ui/toast/toast.service';
import { AuthService } from '../auth/auth.service';
import { AuthState } from '../auth/auth.state';

let isRefreshing = false;
const refreshToken$ = new BehaviorSubject<string | null>(null);

// Never refresh on these
const SKIP_REFRESH_URLS = [
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/logout',
  '/auth/verify-email',
  '/auth/reset-password',
  '/auth/forgot-password'
];

// Never show loader on these
const SKIP_LOADER_URLS = [
  ...SKIP_REFRESH_URLS,
  '/users/me'
];

export const jwtInterceptor: HttpInterceptorFn = ( req: HttpRequest<any>, next: HttpHandlerFn ): Observable<HttpEvent<any>> => {

  const loader = inject(LoaderService);
  const toast = inject(ToastService);
  const authService = inject(AuthService);
  const authState = inject(AuthState);

  const skipRefresh = SKIP_REFRESH_URLS.some(url => req.url.includes(url));
  const skipLoader = SKIP_LOADER_URLS.some(url => req.url.includes(url));

  const token = authState.accessToken();

  // attach Authorization if token exists
  const authReq = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  if (!skipLoader) {
    loader.show();
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {

       if (req.url.includes('/auth/refresh')) {
        return throwError(() => error);
      }
      if (error.status === 401 && skipRefresh) {
        return throwError(() => error);
      }

      if (
        error.status === 404 &&
        req.url.includes('/insurance/shipments')
      ) {
        return throwError(() => error);
      }

      if (
        error.status === 404 &&
        req.url.includes('/drivers/me')
      ) {
        return throwError(() => error);
      }
      if (
        error.status === 401 &&
        !skipRefresh &&
        !!token
      ) {
        return handle401Error(authReq, next, authService, toast);
      }

      if (error.status === 403) {
        toast.error('Access denied.');
      } else if (error.error?.message) {
        toast.error(error.error.message);
      }

      return throwError(() => error);
    }),
    finalize(() => {
      if (!skipLoader) {
        loader.hide();
      }
    })
  );
};


function handle401Error( request: HttpRequest<any>, next: HttpHandlerFn, authService: AuthService, toast: ToastService ): Observable<HttpEvent<any>> {

  if (!isRefreshing) {
    isRefreshing = true;
    refreshToken$.next(null);

    return authService.refreshAccessToken().pipe(
      switchMap(token => {
        isRefreshing = false;

        if (!token) {
          authService.logout();
          return throwError(() => new Error('Session expired'));
        }

        refreshToken$.next(token);

        return next(
          request.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          })
        );
      }),
      catchError(err => {
        isRefreshing = false;
        authService.logout();
        toast.error('Session expired. Please login again.');
        return throwError(() => err);
      })
    );
  }

  return refreshToken$.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap(token =>
      next(
        request.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        })
      )
    )
  );
}
