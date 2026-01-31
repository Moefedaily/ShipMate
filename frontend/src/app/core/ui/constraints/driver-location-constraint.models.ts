export type DriverLocationErrorCode =
  | 'LOCATION_REQUIRED'
  | 'LOCATION_OUTDATED'
  | 'LOCATION_TOO_FAR';

export interface ApiErrorShape {
  message?: string;
  code?: string;
}
