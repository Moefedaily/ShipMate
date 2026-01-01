import { inject } from '@angular/core';
import {
  HttpInterceptorFn,
  HttpErrorResponse
} from '@angular/common/http';
import { finalize, catchError, throwError } from 'rxjs';

import { LoaderService } from '../ui/loader/loader.service';
import { ToastService } from '../ui/toast/toast.service';
import { AuthService } from '../auth/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const loader = inject(LoaderService);
  const toast = inject(ToastService);
  const authService = inject(AuthService);

  loader.show();

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {

      if (error.status === 401) {
        toast.error('Session expired. Please login again.');
        authService.logout();
      } else if (error.status === 403) {
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
