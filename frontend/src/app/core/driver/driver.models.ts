
export enum DriverStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SUSPENDED = 'SUSPENDED'
}

export interface DriverProfileResponse {
  id: string;
  status: DriverStatus;
  vehicleType: string;
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
