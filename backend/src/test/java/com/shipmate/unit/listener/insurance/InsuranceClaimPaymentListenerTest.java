package com.shipmate.unit.listener.insurance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.shipmate.listener.insurance.InsuranceClaimPaymentListener;
import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.listener.payment.PaymentRefundedEvent;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.insuranceClaim.ClaimReason;
import com.shipmate.model.insuranceClaim.ClaimStatus;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.insurance.InsuranceClaimRepository;
import com.shipmate.service.mail.MailService;

@ExtendWith(MockitoExtension.class)
class InsuranceClaimPaymentListenerTest {

    @Mock
    private InsuranceClaimRepository claimRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MailService mailService;

    @InjectMocks
    private InsuranceClaimPaymentListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "strikeThreshold", 3);
    }

    @Test
    void onPaymentRefunded_shouldIgnoreMissingClaim() {
        UUID shipmentId = UUID.randomUUID();
        when(claimRepository.findByShipmentId(shipmentId)).thenReturn(Optional.empty());

        listener.onPaymentRefunded(new PaymentRefundedEvent(shipmentId, UUID.randomUUID(), new BigDecimal("30.00")));

        verify(claimRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onPaymentRefunded_shouldIgnoreAlreadyPaidClaim() {
        InsuranceClaim claim = claim(ClaimStatus.PAID, ClaimReason.DAMAGED, null);
        when(claimRepository.findByShipmentId(claim.getShipment().getId())).thenReturn(Optional.of(claim));

        listener.onPaymentRefunded(new PaymentRefundedEvent(claim.getShipment().getId(), claim.getClaimant().getId(), new BigDecimal("30.00")));

        verify(claimRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onPaymentRefunded_shouldMarkApprovedDamagedClaimAsPaidWithoutStrike() {
        InsuranceClaim claim = claim(ClaimStatus.APPROVED, ClaimReason.DAMAGED, approvedDriverProfile(1, DriverStatus.APPROVED));
        when(claimRepository.findByShipmentId(claim.getShipment().getId())).thenReturn(Optional.of(claim));

        listener.onPaymentRefunded(new PaymentRefundedEvent(claim.getShipment().getId(), claim.getClaimant().getId(), new BigDecimal("30.00")));

        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.PAID);
        assertThat(claim.getResolvedAt()).isNotNull();
        verify(claimRepository).save(claim);
        verify(driverProfileRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onPaymentRefunded_shouldIncrementStrikeForLostClaimWithoutSuspendingBelowThreshold() {
        DriverProfile profile = approvedDriverProfile(1, DriverStatus.APPROVED);
        InsuranceClaim claim = claim(ClaimStatus.APPROVED, ClaimReason.LOST, profile);

        when(claimRepository.findByShipmentId(claim.getShipment().getId())).thenReturn(Optional.of(claim));
        when(driverProfileRepository.findByUser_Id(profile.getUser().getId())).thenReturn(Optional.of(profile));

        listener.onPaymentRefunded(new PaymentRefundedEvent(claim.getShipment().getId(), claim.getClaimant().getId(), new BigDecimal("30.00")));

        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.PAID);
        assertThat(profile.getStrikeCount()).isEqualTo(2);
        assertThat(profile.getStatus()).isEqualTo(DriverStatus.APPROVED);
        verify(driverProfileRepository).save(profile);
        verify(mailService, never()).sendDriverSuspendedEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onPaymentRefunded_shouldSuspendApprovedDriverWhenLostClaimReachesThreshold() {
        DriverProfile profile = approvedDriverProfile(2, DriverStatus.APPROVED);
        InsuranceClaim claim = claim(ClaimStatus.APPROVED, ClaimReason.LOST, profile);

        when(claimRepository.findByShipmentId(claim.getShipment().getId())).thenReturn(Optional.of(claim));
        when(driverProfileRepository.findByUser_Id(profile.getUser().getId())).thenReturn(Optional.of(profile));

        listener.onPaymentRefunded(new PaymentRefundedEvent(claim.getShipment().getId(), claim.getClaimant().getId(), new BigDecimal("30.00")));

        assertThat(profile.getStrikeCount()).isEqualTo(3);
        assertThat(profile.getStatus()).isEqualTo(DriverStatus.SUSPENDED);
        verify(driverProfileRepository, org.mockito.Mockito.times(2)).save(profile);
        verify(mailService).sendDriverSuspendedEmail(profile.getUser().getEmail());

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationRequestedEvent::title)
                .contains("Insurance refund completed", "Account Suspended");
    }

    private InsuranceClaim claim(ClaimStatus status, ClaimReason reason, DriverProfile driverProfile) {
        User claimant = User.builder()
                .id(UUID.randomUUID())
                .email("claimant@test.com")
                .firstName("Claim")
                .lastName("User")
                .build();

        User driver = driverProfile != null ? driverProfile.getUser() : null;

        Booking booking = Booking.builder()
                .driver(driver)
                .build();

        Shipment shipment = Shipment.builder()
                .id(UUID.randomUUID())
                .sender(claimant)
                .booking(booking)
                .build();

        return InsuranceClaim.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .claimant(claimant)
                .claimStatus(status)
                .claimReason(reason)
                .declaredValueSnapshot(new BigDecimal("100.00"))
                .coverageAmount(new BigDecimal("90.00"))
                .deductibleRate(new BigDecimal("0.10"))
                .compensationAmount(new BigDecimal("90.00"))
                .createdAt(Instant.now())
                .build();
    }

    private DriverProfile approvedDriverProfile(int strikeCount, DriverStatus status) {
        User driver = User.builder()
                .id(UUID.randomUUID())
                .email("driver@test.com")
                .firstName("Driver")
                .lastName("User")
                .build();

        return DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(driver)
                .status(status)
                .strikeCount(strikeCount)
                .build();
    }
}
