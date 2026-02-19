export type PaymentStatus =
  | 'REQUIRED'
  | 'PROCESSING'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'FAILED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface CreatePaymentIntentResponse {
  clientSecret: string;
  paymentStatus: PaymentStatus;
  amountTotal: number;
  currency: string;
}

export interface PaymentResponse {
  shipmentId: string;
  paymentStatus: PaymentStatus;
  amountTotal: number;
  currency: string;
  failureReason?: string;
}
