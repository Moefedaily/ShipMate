
import { VehicleType } from '../driver.models';
export type VehicleStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Vehicle {
  id: string;
  vehicleType: VehicleType;
  maxWeightCapacity: number;
  plateNumber?: string;
  insuranceExpiry?: string;
  vehicleDescription?: string;
  rejectionReason?: string;
  status: VehicleStatus;
  active: boolean;
  createdAt: string;
}

export interface CreateVehicleRequest {
  vehicleType: VehicleType;
  maxWeightCapacity: number;
  plateNumber?: string;
  insuranceExpiry?: string;
  vehicleDescription?: string;
}
