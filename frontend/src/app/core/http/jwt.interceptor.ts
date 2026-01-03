import { inject } from '@angular/core';
import {
  HttpInterceptorFn,
  HttpErrorResponse,
  HttpEvent,
  HttpRequest,
  HttpHandlerFn
} from '@angular/common/http';
import {
  BehaviorSubject,
  catchError,
  filter,
  finalize,
  switchMap,
  take,
  throwError,
  Observable
} from 'rxjs';

import { LoaderService } from '../ui/loader/loader.service';
import { ToastService } from '../ui/toast/toast.service';
import { AuthService } from '../auth/auth.service';
import { AuthState } from '../auth/auth.state';

let isRefreshing = false;
const refreshToken$ = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = ( req: HttpRequest<any>, next: HttpHandlerFn ): Observable<HttpEvent<any>> => {

  const loader = inject(LoaderService);
  const toast = inject(ToastService);
  const authService = inject(AuthService);
  const authState = inject(AuthState);

  const token = authState.accessToken();

  const authReq = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  loader.show();

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status === 401 &&
        !authReq.url.includes('/auth/refresh')
      ) {
        return handle401Error(authReq, next, authService, authState, toast);
      }

      if (error.status === 403) {
        toast.error('Access denied.');
      } else if (error.error?.message) {
        toast.error(error.error.message);
      } else {
        toast.error('Unexpected error occurred.');
      }

      return throwError(() => error);
    }),
    finalize(() => loader.hide())
  );
};

/**
 * Handle 401 by refreshing token once and retrying the request
 */
function handle401Error( request: HttpRequest<any>, next: HttpHandlerFn, authService: AuthService, authState: AuthState,
  toast: ToastService ): Observable<HttpEvent<any>> {

  if (!isRefreshing) {
    isRefreshing = true;
    refreshToken$.next(null);

    return authService.refreshAccessToken().pipe(
      switchMap(token => {
        isRefreshing = false;

        if (!token) {
          authService.logout();
          toast.error('Session expired. Please login again.');
          return throwError(() => new Error('Session expired'));
        }

        refreshToken$.next(token);

        const retryReq = request.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });

        return next(retryReq);
      }),
      catchError(err => {
        isRefreshing = false;
        authService.logout();
        toast.error('Session expired. Please login again.');
        return throwError(() => err);
      })
    );
  }

  // Wait for refresh to complete, then retry
  return refreshToken$.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap(token => {
      const retryReq = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      return next(retryReq);
    })
  );
}
