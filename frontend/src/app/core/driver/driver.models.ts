
/**
 * DriverStatus represents the BACKEND / DOMAIN state.
 * 
 * - Comes directly from the API and database
 * - Used for business rules, permissions, and lifecycle enforcement
 * - MUST NOT include UI-only states
 * - MUST NOT be mutated by the frontend
 */
export enum DriverStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SUSPENDED = 'SUSPENDED'
}

export interface DriverProfileResponse {
  id: string;
  status: DriverStatus;
  vehicleType: VehicleType;
  maxWeightCapacity: number;
  approvedAt: string | null;
}

export interface DriverApplyRequest {
  licenseNumber: string;
  vehicleType: string;
  maxWeightCapacity: number;
  vehicleDescription: string;
}

export enum VehicleType {
  CAR = 'CAR',
  VAN = 'VAN',
  MOTORCYCLE = 'MOTORCYCLE',
  TRUCK = 'TRUCK',
  BICYCLE = 'BICYCLE',
}

export const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  [VehicleType.CAR]: 'Car',
  [VehicleType.VAN]: 'Van',
  [VehicleType.MOTORCYCLE]: 'Motorcycle',
  [VehicleType.TRUCK]: 'Truck',
  [VehicleType.BICYCLE]: 'Bicycle',
};
