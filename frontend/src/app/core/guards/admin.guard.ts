import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthState } from '../../core/auth/auth.state';

export const adminGuard: CanActivateFn = () => {
  const authState = inject(AuthState);
  const router    = inject(Router);

  if (!authState.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  const user = authState.user();
  if (user?.role !== 'ADMIN') {
    return router.parseUrl('/forbidden');
  }

  return true;
};