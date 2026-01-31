import { ApiErrorShape, DriverLocationErrorCode } from "./driver-location-constraint.models";


export class DriverLocationConstraintHandler {
  static isLocationError(err: unknown): err is { error?: ApiErrorShape } {
    const e = err as any;
    const code = e?.error?.code;
    return (
      code === 'LOCATION_REQUIRED' ||
      code === 'LOCATION_OUTDATED' ||
      code === 'LOCATION_TOO_FAR'
    );
  }

  static getModalTitle(code: DriverLocationErrorCode): string {
    switch (code) {
      case 'LOCATION_REQUIRED':
        return 'Turn on your location to continue';
      case 'LOCATION_OUTDATED':
        return 'Refresh your location to continue';
      case 'LOCATION_TOO_FAR':
        return 'Update your location to confirm this delivery';
      default:
        return 'Update your location';
    }
  }

  static getModalBody(code: DriverLocationErrorCode): string {
    switch (code) {
      case 'LOCATION_REQUIRED':
        return 'We need your location to show nearby deliveries and confirm feasibility.';
      case 'LOCATION_OUTDATED':
        return 'Your saved location is out of date. Please refresh it to continue.';
      case 'LOCATION_TOO_FAR':
        return 'Based on your last known location, you may be too far from the pickup. Update your location to confirm.';
      default:
        return 'Please update your location to continue.';
    }
  }

  static getCode(err: unknown): DriverLocationErrorCode | null {
    if (!this.isLocationError(err)) return null;
    return (err as any).error.code as DriverLocationErrorCode;
  }
}
