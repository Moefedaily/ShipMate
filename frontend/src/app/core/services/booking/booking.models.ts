export interface CreateBookingRequest {
  shipmentIds: string[];
}

export interface BookingResponse {
  id: string;
  status: 'PENDING' | 'CONFIRMED' | 'IN_PROGRESS';
  totalPrice: number;
  driverEarnings: number;
  shipments: {
    id: string;
    pickupAddress: string;
    deliveryAddress: string;
  }[];
}

