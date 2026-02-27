export type PayoutStatus = 'PENDING' | 'PAID';
export type EarningType = 'ORIGINAL' | 'REFUND';

export interface DriverEarningResponse {
  id: string;
  shipmentId: string;
  paymentId: string;
  grossAmount: number;
  commissionAmount: number;
  netAmount: number;
  payoutStatus: PayoutStatus;
  earningType: EarningType;
  createdAt: string;
}

export interface DriverEarningsSummaryResponse {
  totalGross: number;
  totalCommission: number;
  totalNet: number;
  totalPending: number;
  totalPaid: number;
}
