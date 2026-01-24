import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { DriverService } from '../driver/driver.service';

export const driverApprovedGuard: CanActivateFn = () => {
  const driverService = inject(DriverService);
  const router = inject(Router);

  return driverService.getMyDriverProfile().pipe(
    map(profile => {
      if (profile.status === 'APPROVED') {
        return true;
      }

      // Driver exists but not approved
      return router.parseUrl('/dashboard/driver');
    }),
    catchError(err => {
      // 404 → no driver profile → redirect to driver home (apply / info)
      if (err.status === 404) {
        return of(router.parseUrl('/dashboard/driver'));
      }

      // fallback
      return of(router.parseUrl('/'));
    })
  );
};
