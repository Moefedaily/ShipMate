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
}
