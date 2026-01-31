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
  status: 'CREATED' | 'PENDING' | 'ASSIGNED' | 'PICKED_UP' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED';
  photos: string[] | null;
  createdAt: string;
  updatedAt: string;
}