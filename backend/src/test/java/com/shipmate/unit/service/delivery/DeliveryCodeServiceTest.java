package com.shipmate.unit.service.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.shipmate.dto.response.delivery.DeliveryCodeStatusResponse;
import com.shipmate.listener.delivery.DeliveryUnlockedEvent;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.service.delivery.DeliveryCodeAttemptService;
import com.shipmate.service.delivery.DeliveryCodeService;

@ExtendWith(MockitoExtension.class)
class DeliveryCodeServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DeliveryCodeAttemptService attemptService;

    @InjectMocks
    private DeliveryCodeService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hmacSecret", "test-hmac-secret");
        ReflectionTestUtils.setField(service, "aesKeyBase64", Base64.getEncoder().encodeToString("0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(service, "ttlMinutes", 15L);
    }

    @Test
    void generateAndStore_shouldPersistNewCodeForAssignedShipment() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        String code = service.generateAndStore(shipment);

        assertThat(code).matches("\\d{6}");
        assertThat(shipment.getDeliveryCodeHash()).isNotBlank();
        assertThat(shipment.getDeliveryCodeEnc()).isNotBlank();
        assertThat(shipment.getDeliveryCodeIv()).isNotBlank();
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void generateAndStore_shouldRejectWrongStatus() {
        Shipment shipment = shipment(ShipmentStatus.CREATED);

        assertThatThrownBy(() -> service.generateAndStore(shipment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ASSIGNED");
    }

    @Test
    void verify_shouldConfirmMatchingCode() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        when(shipmentRepository.save(shipment)).thenReturn(shipment);
        String code = service.generateAndStore(shipment);
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        clearInvocations(shipmentRepository);

        service.verify(shipment, code);

        assertThat(shipment.getDeliveryCodeVerifiedAt()).isNotNull();
        assertThat(shipment.getDeliveryCodeAttempts()).isZero();
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void verify_shouldRejectInvalidCodeAndIncrementAttempts() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        when(shipmentRepository.save(shipment)).thenReturn(shipment);
        service.generateAndStore(shipment);
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(attemptService.incrementAttemptsAndLockIfNeeded(shipment.getId(), 5)).thenReturn(1);

        assertThatThrownBy(() -> service.verify(shipment, "999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid delivery code");

        verify(attemptService).incrementAttemptsAndLockIfNeeded(shipment.getId(), 5);
    }

    @Test
    void getActiveCode_shouldReturnDecryptedCodeForSender() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        when(shipmentRepository.save(shipment)).thenReturn(shipment);
        String code = service.generateAndStore(shipment);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        DeliveryCodeStatusResponse response = service.getActiveCode(shipment.getId(), shipment.getSender().getId());

        assertThat(response.code()).isEqualTo(code);
        assertThat(response.shipmentId()).isEqualTo(shipment.getId());
    }

    @Test
    void getActiveCode_shouldRejectWrongSender() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.getActiveCode(shipment.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized");
    }

    @Test
    void reset_shouldClearExistingCodeAndGenerateNewOne() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        when(shipmentRepository.save(shipment)).thenReturn(shipment);
        String original = service.generateAndStore(shipment);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.of(Payment.builder()
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .build()));

        String reset = service.reset(shipment.getId(), shipment.getSender().getId());

        assertThat(reset).matches("\\d{6}");
        assertThat(reset).isNotEqualTo(original);
        verify(shipmentRepository, org.mockito.Mockito.atLeastOnce()).save(shipment);
    }

    @Test
    void unlockByAdmin_shouldUnlockLockedShipmentAndPublishEvent() {
        Shipment shipment = shipment(ShipmentStatus.IN_TRANSIT);
        shipment.setDeliveryLocked(true);
        shipment.setDeliveryCodeAttempts(5);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        service.unlockByAdmin(shipment.getId());

        assertThat(shipment.isDeliveryLocked()).isFalse();
        assertThat(shipment.getDeliveryCodeAttempts()).isZero();
        verify(eventPublisher).publishEvent(any(DeliveryUnlockedEvent.class));
    }

    @Test
    void unlockByAdmin_shouldRejectUnlockedShipment() {
        Shipment shipment = shipment(ShipmentStatus.IN_TRANSIT);
        shipment.setDeliveryLocked(false);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.unlockByAdmin(shipment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Shipment is not locked");

        verify(eventPublisher, never()).publishEvent(any());
    }

    private Shipment shipment(ShipmentStatus status) {
        User sender = User.builder().id(UUID.randomUUID()).build();
        User driver = User.builder().id(UUID.randomUUID()).build();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .shipments(Set.of())
                .build();

        return Shipment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .booking(booking)
                .status(status)
                .build();
    }
}
