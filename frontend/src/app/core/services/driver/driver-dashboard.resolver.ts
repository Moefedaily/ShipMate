import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { DriverService } from './driver.service';
import { DriverDashboardState } from '../../../features/dashboard/driver/state/driver-dashboard.state';
import { DriverStatus } from './driver.models';

export const driverDashboardResolver: ResolveFn<DriverDashboardState> = () => {
  const driverService = inject(DriverService);

  return driverService.getMyDriverProfile().pipe(
    map(profile => {

      if (profile.status === DriverStatus.APPROVED) {
        if (!profile.lastLatitude || !profile.lastLongitude) {
          return DriverDashboardState.LOCATION_REQUIRED;
        }
        return DriverDashboardState.APPROVED;
      }

      switch (profile.status) {
        case DriverStatus.PENDING:
          return DriverDashboardState.PENDING;

        case DriverStatus.REJECTED:
          return DriverDashboardState.REJECTED;

        case DriverStatus.SUSPENDED:
          return DriverDashboardState.SUSPENDED;

        default:
          return DriverDashboardState.NOT_APPLIED;
      }
    }),
    catchError(err => {
      if (err.status === 404) {
        return of(DriverDashboardState.NOT_APPLIED);
      }
      throw err;
    })
  );
};
