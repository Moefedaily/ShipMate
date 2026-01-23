import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthState } from '../auth/auth.state';

export const driverGuard: CanActivateFn = () => {
  const authState = inject(AuthState);
  const router = inject(Router);

  if (!authState.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  if (!authState.isDriver()) {
    return router.parseUrl('/forbidden');
  }

  return true;
};
