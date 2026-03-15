
import { Vehicle } from './vehicle/vehicle.models';

export enum DriverStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SUSPENDED = 'SUSPENDED'
}

export interface DriverProfileResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  licenseNumber: string;
  licensePhotoUrl: string | null;
  licensePhotoUrls: string[];
  licenseExpiry: string | null;
  status: DriverStatus;
  strikeCount: number;
  createdAt: string;
  approvedAt: string | null;
  vehicles: Vehicle[];
  activeVehicle: Vehicle | null;
  lastLatitude: number | null;
  lastLongitude: number | null;
  lastLocationUpdatedAt: string | null;
}

export interface DriverApplyRequest {
  licenseNumber: string;
  licenseExpiry: string;
  vehicleType: VehicleType;
  maxWeightCapacity: number;
  plateNumber?: string;
  vehicleDescription?: string;
}

export interface UpdateLicenseRequest {
  licenseNumber: string;
  licenseExpiry: string;
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
