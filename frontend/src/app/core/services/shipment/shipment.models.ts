export interface CreateShipmentRequest {
  pickupAddress: string;
  pickupLatitude: number;
  pickupLongitude: number;

  deliveryAddress: string;
  deliveryLatitude: number;
  deliveryLongitude: number;

  packageDescription?: string;
  packageWeight: number;
  packageValue: number;

  requestedPickupDate: string;
  requestedDeliveryDate: string;

  basePrice: number;
}

export interface AssignedDriver {
  id: string;
  firstName: string;
  lastName: string;
  avatarUrl: string | null;
  vehicleType: 'CAR' | 'VAN' | 'TRUCK';
}
export interface ShipmentResponse {
  id: string;
  senderId: string;
  pickupAddress: string;
  pickupLatitude: number;          
  pickupLongitude: number;
  deliveryAddress: string;
  deliveryLatitude: number; 
  deliveryLongitude: number;
  pickupOrder: number | null;
  deliveryOrder: number | null;
  packageDescription: string;
  packageWeight: number;
  packageValue: number;
  requestedPickupDate: string;
  requestedDeliveryDate: string;
  basePrice: number;
  extraInsuranceFee: number;
  status: 'CREATED' | 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED';
  photos: string[] | null;
  createdAt: string;
  updatedAt: string;
  driver?: AssignedDriver | null;
}

export interface UpdateShipmentRequest {
  pickupAddress: string;
  deliveryAddress: string;

  packageDescription?: string;
  packageWeight: number;

  requestedPickupDate: string;
  requestedDeliveryDate: string;
}
