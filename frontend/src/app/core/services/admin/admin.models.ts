// admin.models.ts
import { PaymentStatus } from '../payment/payment.model';

export interface AdminDashboardStats {
  totalUsers: number;
  totalDrivers: number;
  activeShipments: number;
  completedShipments: number;
  pendingClaims: number;
  totalPayments: number;
  totalRevenue: number;
}

export type UserType = 'SENDER' | 'DRIVER' | 'BOTH';
export type Role = 'ADMIN' | 'USER';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/* =========================
   USERS
========================= */

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  role: Role;
  userType: UserType;
  verified: boolean;
  active: boolean;
  avatarUrl?: string | null;
  createdAt: string;
}

export interface UserFilterParams {
  role?: Role;
  userType?: UserType;
  active?: boolean;
  search?: string;
  page?: number;
  size?: number;
}

export interface UserShipmentRow {
  id: string;
  pickupAddress: string;
  deliveryAddress: string;
  status: ShipmentStatus;
  createdAt: string;
}

export interface UserClaimRow {
  id: string;
  claimStatus: ClaimStatus;
  compensationAmount: number;
  declaredValueSnapshot: number;
  createdAt: string;
}

export interface UserPaymentRow {
  id: string;
  paymentStatus: PaymentStatus;
  amountTotal: number;
  currency: string;
  createdAt: string;
}

/* =========================
   DRIVERS (matches backend DriverProfileResponse)
========================= */

export type DriverStatus = 'PENDING' | 'APPROVED' | 'SUSPENDED' | 'REJECTED';

export type VehicleType =
  | 'CAR'
  | 'VAN'
  | 'MOTORCYCLE'
  | 'TRUCK'
  | 'BICYCLE';

export interface AdminDriverProfile {
  id: string;
  licenseNumber: string;
  vehicleType: VehicleType;
  maxWeightCapacity: number;
  vehicleDescription: string;
  status: DriverStatus;

  lastLatitude: number | null;
  lastLongitude: number | null;
  lastLocationUpdatedAt: string | null;

  createdAt: string;
  approvedAt: string | null;

  strikeCount: number;
}

export interface DriverFilterParams {
  status?: DriverStatus;
  page?: number;
  size?: number;
}

/** request body for POST /admin/drivers/{id}/strikes */
export interface AdminStrikeRequest {
  note: string;
}

/* =========================
   SHIPMENTS
========================= */

export type ShipmentStatus =
  | 'CREATED'
  | 'ASSIGNED'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'LOST';

export interface ShipmentFilterParams {
  status?: ShipmentStatus;
  page?: number;
  size?: number;
}

export interface AdminShipment {
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

  insuranceSelected: boolean;
  insuranceFee: number;
  declaredValue?: number;
  insuranceCoverageAmount?: number;

  status: ShipmentStatus;

  createdAt: string;
  updatedAt: string;

  driver?: {
    id: string;
    firstName: string;
    lastName: string;
    email?: string; // if you added it in AssignedDriverResponse
    avatarUrl?: string | null;
    vehicleType?: VehicleType;
  } | null;

  sender?: {
    id: string;
    firstName: string;
    lastName: string;
    email?: string;
  } | null;
}

export interface AdminUpdateShipmentStatusRequest {
  status: ShipmentStatus;
  adminNotes?: string;
}

/* =========================
   CLAIMS
========================= */

export type ClaimStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'PAID';

export interface ClaimFilterParams {
  status?: ClaimStatus;
  page?: number;
  size?: number;
}

export interface AdminClaim {
  id: string;

  shipmentId: string;
  claimantId: string;

  declaredValueSnapshot: number;
  coverageAmount: number;
  deductibleRate: number;
  compensationAmount: number;

  claimReason: 'DAMAGED' | 'LOST' | 'OTHER';
  claimStatus: ClaimStatus;

  description?: string;
  photos: string[];

  adminNotes?: string;

  createdAt: string;
  resolvedAt?: string;
}

export interface AdminClaimDecisionRequest {
  adminNotes?: string;
}

/* =========================
   PAYMENTS
========================= */

export interface PaymentFilterParams {
  status?: PaymentStatus;
  page?: number;
  size?: number;
}

export interface AdminPayment {
  id: string;
  shipmentId: string;
  senderId: string;
  amountTotal: number;
  currency: string;
  paymentStatus: PaymentStatus;
  stripePaymentIntentId?: string | null;
  createdAt: string;
}

/* =========================
   BOOKINGS
========================= */
export interface AdminBooking {
  id: string;
  shipmentId: string;
  driverId: string;
  status: string;
  createdAt: string;
}

export interface BookingFilterParams {
  page?: number;
  size?: number;
}

/* =========================
   EARNINGS
========================= */
export interface AdminEarning {
  id: string;
  driverId: string;
  amount: number;
  currency: string;
  paid: boolean;
  createdAt: string;
}

