
import { DriverDashboardState } from '../../../features/dashboard/driver/state/driver-dashboard.state';
import { DriverStatus } from './driver.models';

export function mapDriverStatusToDashboardState(
  status?: DriverStatus
): DriverDashboardState {

  if (!status) {
    return DriverDashboardState.NOT_APPLIED;
  }

  switch (status) {
    case DriverStatus.PENDING:
      return DriverDashboardState.PENDING;

    case DriverStatus.APPROVED:
      return DriverDashboardState.APPROVED;

    case DriverStatus.REJECTED:
      return DriverDashboardState.REJECTED;

    case DriverStatus.SUSPENDED:
      return DriverDashboardState.SUSPENDED;

    default:
      return DriverDashboardState.NOT_APPLIED;
  }
}
