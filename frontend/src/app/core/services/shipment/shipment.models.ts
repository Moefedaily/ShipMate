import { PaymentStatus } from "../payment/payment.model";

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
  paymentStatus: PaymentStatus;
  pickupAddress: string;
  pickupLatitude: number;          
  pickupLongitude: number;
  deliveryAddress: string;
  deliveryLatitude: number; 
  deliveryLongitude: number;
  deliveryLocked: boolean;
  deliveryCodeAttempts: number;
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

export interface PricingEstimateRequest {
  pickupLatitude: number;
  pickupLongitude: number;
  deliveryLatitude: number;
  deliveryLongitude: number;
  packageWeight: number;
}

export interface PricingEstimateResponse {
  distanceKm: number;
  estimatedBasePrice: number;
}

export interface PageResponse<T> {
  content: T[];

  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };

  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  numberOfElements: number;
  size: number;
  number: number;

  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };

  empty: boolean;
}
export interface DeliveryCodeStatusResponse {
  shipmentId: string;
  code: string;
  expiresAt: string;
}
