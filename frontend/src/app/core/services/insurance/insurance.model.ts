import { PhotoResponse } from "../../../shared/models/photo.models";

export type ClaimReason = 'DAMAGED' | 'LOST' | 'OTHER';

export type ClaimStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'PAID';

export interface InsuranceClaim {
  id: string;
  shipmentId: string;

  declaredValueSnapshot: number;
  coverageAmount: number;
  deductibleRate: number;
  compensationAmount: number;

  claimReason: ClaimReason;
  claimStatus: ClaimStatus;

  description?: string;
  photos: PhotoResponse[];

  adminNotes?: string | null;

  createdAt: string;
  resolvedAt?: string | null;
}

export interface CreateInsuranceClaimRequest {
  claimReason: ClaimReason;
  description?: string;
  photos?: string[];
}
