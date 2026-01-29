/**
 * DriverDashboardState represents the FRONTEND UI state machine.
 *
 * - Used ONLY for presentation and component rendering
 * - Can include states that do NOT exist in the backend
 *   (e.g. INIT, NOT_APPLIED, APPLYING)
 * - Derived from DriverStatus via a mapper / resolver
 * - MUST NOT be sent to the backend
 */
export enum DriverDashboardState {
  INIT = 'INIT',

  NOT_APPLIED = 'NOT_APPLIED',
  APPLYING = 'APPLYING',
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SUSPENDED = 'SUSPENDED',

  LOCATION_REQUIRED = 'LOCATION_REQUIRED'
}