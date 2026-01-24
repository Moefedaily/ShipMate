import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { DriverService } from './driver.service';
import { DriverDashboardState } from '../../features/dashboard/driver/state/driver-dashboard.state';
import { mapDriverStatusToDashboardState } from './driver.mapper';

export const driverDashboardResolver: ResolveFn<DriverDashboardState> = () => {
  const driverService = inject(DriverService);
  const router = inject(Router);

  return driverService.getMyDriverProfile().pipe(
    map(profile =>
      mapDriverStatusToDashboardState(profile.status)
    ),
    catchError(err => {
      if (err.status === 404) {
        return of(DriverDashboardState.NOT_APPLIED);
      }

      return of(DriverDashboardState.NOT_APPLIED);
    })
  );
};
