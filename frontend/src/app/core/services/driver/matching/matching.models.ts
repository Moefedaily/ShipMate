export interface Shipment {
  id: string;

  senderId: string;

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

  status: 'CREATED' | 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED';

  createdAt: string;
  updatedAt: string;
}


export interface MatchingMetrics {
  distanceToPickupKm: number;
  pickupToDeliveryKm: number;
  estimatedDetourKm: number | null;
  score: number;
}


export interface MatchResult {
  shipment: Shipment;
  metrics: MatchingMetrics;
}
