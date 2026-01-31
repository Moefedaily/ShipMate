import { ShipmentResponse } from "../shipment/shipment.models";

export interface CreateBookingRequest {
  shipmentIds: string[];
}

export interface BookingResponse {
  id: string;
  driverId: string;
  status: 'PENDING' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  totalPrice: number;
  platformCommission: number;
  driverEarnings: number;
  shipments: ShipmentResponse[];
  createdAt: string;
}

