package com.shipmate.service.admin;

import com.shipmate.model.admin.*;
import com.shipmate.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminActionLogger {

    private final AdminAuditService audit;

    private UUID adminId() {

        try {
            return SecurityUtils.getCurrentUserId();
        } catch (IllegalStateException ex) {

            // tests
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }
    private void log(AdminTargetType target, UUID targetId, AdminActionType action, String note) {
        audit.log(
            adminId(),
            target,
            targetId,
            action,
            note
        );
    }

    // ===== SHIPMENTS =====

    public void shipmentCancelled(UUID shipmentId, String note) {
        log(AdminTargetType.SHIPMENT, shipmentId, AdminActionType.SHIPMENT_CANCELLED, note);
    }

    public void shipmentMarkedLost(UUID shipmentId, String note) {
        log(AdminTargetType.SHIPMENT, shipmentId, AdminActionType.SHIPMENT_MARKED_LOST, note);
    }

    // ===== DRIVERS =====

    public void driverApproved(UUID driverId, String note) {
        log(AdminTargetType.DRIVER, driverId, AdminActionType.DRIVER_APPROVED, note);
    }

    public void driverRejected(UUID driverId, String note) {
        log(AdminTargetType.DRIVER, driverId, AdminActionType.DRIVER_REJECTED, note);
    }

    public void driverSuspended(UUID driverId, String note) {
        log(AdminTargetType.DRIVER, driverId, AdminActionType.DRIVER_SUSPENDED, note);
    }

    public void driverStrike(UUID driverId, String note) {
        log(AdminTargetType.DRIVER, driverId, AdminActionType.DRIVER_STRIKE, note);
    }

    public void driverResetStrikes(UUID driverId, String note) {
        log(AdminTargetType.DRIVER, driverId, AdminActionType.DRIVER_RESET_STRIKES, note);
    }

    // ===== CLAIMS =====

    public void claimApproved(UUID claimId, String note) {
        log(AdminTargetType.CLAIM, claimId, AdminActionType.CLAIM_APPROVED, note);
    }

    public void claimRejected(UUID claimId, String note) {
        log(AdminTargetType.CLAIM, claimId, AdminActionType.CLAIM_REJECTED, note);
    }

    // ===== PAYMENTS =====

    public void paymentRefunded(UUID paymentId, String note) {
        log(AdminTargetType.PAYMENT, paymentId, AdminActionType.PAYMENT_REFUNDED, note);
    }

    // ===== USERS =====

    public void userActivated(UUID userId, String note) {
        log(AdminTargetType.USER, userId, AdminActionType.USER_ACTIVATED, note);
    }

    public void userDeactivated(UUID userId, String note) {
        log(AdminTargetType.USER, userId, AdminActionType.USER_DEACTIVATED, note);
    }

    public void shipmentStatusOverride(UUID shipmentId, String note) {
    log( AdminTargetType.SHIPMENT, shipmentId, AdminActionType.SHIPMENT_STATUS_OVERRIDE, note );
}
}