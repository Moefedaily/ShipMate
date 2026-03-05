import { ClaimStatus, DriverStatus, ShipmentStatus } from "../../../core/services/admin/admin.models";
import { PaymentStatus } from "../../../core/services/payment/payment.model";
import { BadgeVariant } from "../../../features/dashboard/admin/components/admin-badge/admin-badge.component";

export function shipmentStatusBadge(status: ShipmentStatus): { label: string; variant: BadgeVariant } {
  const map: Record<ShipmentStatus, { label: string; variant: BadgeVariant }> = {
    CREATED:    { label: 'Created',    variant: 'info'   },
    ASSIGNED:  { label: 'Assigned',  variant: 'primary'   },
    IN_TRANSIT: { label: 'In Transit', variant: 'warning' },
    DELIVERED:  { label: 'Delivered',  variant: 'success' },
    CANCELLED: { label: 'Cancelled', variant: 'danger' },
    LOST:       { label: 'Lost',       variant: 'danger'  },
  };
  return map[status] ?? { label: status, variant: 'muted' };
}

export function claimStatusBadge(status: ClaimStatus): { label: string; variant: BadgeVariant } {
  const map: Record<ClaimStatus, { label: string; variant: BadgeVariant }> = {
    SUBMITTED:    { label: 'Submitted',    variant: 'info'    },
    UNDER_REVIEW: { label: 'Under Review', variant: 'warning' },
    APPROVED:     { label: 'Approved',     variant: 'success' },
    REJECTED:     { label: 'Rejected',     variant: 'danger'  },
    PAID:         { label: 'Paid',         variant: 'primary' },
  };
  return map[status] ?? { label: status, variant: 'muted' };
}

export function paymentStatusBadge(status: PaymentStatus): { label: string; variant: BadgeVariant } {
  const map: Record<PaymentStatus, { label: string; variant: BadgeVariant }> = {
    AUTHORIZED: { label: 'Authorized', variant: 'info'    },
    CAPTURED:   { label: 'Captured',   variant: 'success' },
    REFUNDED:   { label: 'Refunded',   variant: 'warning' },
    FAILED:     { label: 'Failed',     variant: 'danger'  },
    CANCELLED: { label: 'Cancelled', variant: 'danger' },
    REQUIRED:   { label: 'Required',   variant: 'primary' },
    PROCESSING: { label: 'Processing', variant: 'info'    },
  };
  return map[status] ?? { label: status, variant: 'muted' };
}

export function driverStatusBadge(status: DriverStatus): { label: string; variant: BadgeVariant } {
  const map: Record<DriverStatus, { label: string; variant: BadgeVariant }> = {
    PENDING:   { label: 'Pending',   variant: 'warning' },
    APPROVED:  { label: 'Approved',  variant: 'success' },
    SUSPENDED: { label: 'Suspended', variant: 'danger'  },
    REJECTED:  { label: 'Rejected',  variant: 'muted'   },
  };
  return map[status] ?? { label: status, variant: 'muted' };
}