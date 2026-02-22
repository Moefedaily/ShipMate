package com.shipmate.listener.insurance;

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.listener.payment.PaymentRefundedEvent;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.insuranceClaim.ClaimReason;
import com.shipmate.model.insuranceClaim.ClaimStatus;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.insurance.InsuranceClaimRepository;
import com.shipmate.service.mail.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceClaimPaymentListener {

    private final InsuranceClaimRepository claimRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MailService mailService;

    @Value("${app.driver.strike-threshold:3}")
    private int strikeThreshold;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {

        claimRepository.findByShipmentId(event.shipmentId())
            .ifPresent(claim -> {

                // Idempotency: already processed
                if (claim.getClaimStatus() == ClaimStatus.PAID) {
                    return;
                }

                // Only process approved claims
                if (claim.getClaimStatus() != ClaimStatus.APPROVED) {
                    return;
                }

                // 1) Mark claim as PAID (refund confirmed)
                claim.setClaimStatus(ClaimStatus.PAID);
                claim.setResolvedAt(Instant.now());
                claimRepository.save(claim);

                log.info(
                    "[INSURANCE] Claim PAID shipmentId={} claimId={}",
                    event.shipmentId(),
                    claim.getId()
                );

                // 2) Strikes only for LOST
                if (claim.getClaimReason() != ClaimReason.LOST) {
                    return;
                }

                // 3) Need driver context
                var shipment = claim.getShipment();
                if (shipment.getBooking() == null || shipment.getBooking().getDriver() == null) {
                    return;
                }

                var driver = shipment.getBooking().getDriver();

                DriverProfile profile = driverProfileRepository
                    .findByUser_Id(driver.getId())
                    .orElseThrow(() -> new IllegalStateException("Driver profile not found"));
				int previousStrikeCount = profile.getStrikeCount();
				int newStrikeCount = previousStrikeCount + 1;

				profile.setStrikeCount(newStrikeCount);
				driverProfileRepository.save(profile);
				log.warn(
					"[STRIKE] Driver {} strike incremented {} -> {}",
					driver.getId(),
					previousStrikeCount,
					newStrikeCount
				);

				if (previousStrikeCount < strikeThreshold
					&& newStrikeCount >= strikeThreshold
					&& profile.getStatus() == DriverStatus.APPROVED) {
					profile.setStatus(DriverStatus.SUSPENDED);
					driverProfileRepository.save(profile);
					log.error(
						"[SUSPEND] Driver {} auto-suspended after {} strikes",
						driver.getId(),
						newStrikeCount
					);
					eventPublisher.publishEvent(
						new NotificationRequestedEvent(
							driver.getId(),
							"Account Suspended",
							"Your driver account has been suspended due to multiple confirmed lost shipments. Please contact support.",
							NotificationType.SYSTEM_ALERT,
							null,
							ReferenceType.SYSTEM
						)
					);
					mailService.sendDriverSuspendedEmail(driver.getEmail());
				}
            });
    }
}