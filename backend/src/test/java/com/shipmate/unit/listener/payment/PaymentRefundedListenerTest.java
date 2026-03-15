package com.shipmate.unit.listener.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.listener.payment.PaymentRefundedEvent;
import com.shipmate.listener.payment.PaymentRefundedListener;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import com.shipmate.repository.notification.NotificationRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.service.mail.MailService;

@ExtendWith(MockitoExtension.class)
class PaymentRefundedListenerTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MailService mailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentRefundedListener listener;

    @Test
    void onPaymentRefunded_shouldSkipDuplicateNotification() {
        Shipment shipment = shipment();
        PaymentRefundedEvent event = new PaymentRefundedEvent(shipment.getId(), shipment.getSender().getId(), new BigDecimal("42.00"));

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(notificationRepository.existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                shipment.getSender().getId(),
                shipment.getId(),
                ReferenceType.SHIPMENT,
                "Payment Refunded"
        )).thenReturn(true);

        listener.onPaymentRefunded(event);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
        verify(mailService, never()).sendPaymentRefundedEmail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onPaymentRefunded_shouldNotifyAndSendMail() {
        Shipment shipment = shipment();
        BigDecimal amount = new BigDecimal("42.00");
        PaymentRefundedEvent event = new PaymentRefundedEvent(shipment.getId(), shipment.getSender().getId(), amount);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(notificationRepository.existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                shipment.getSender().getId(),
                shipment.getId(),
                ReferenceType.SHIPMENT,
                "Payment Refunded"
        )).thenReturn(false);

        listener.onPaymentRefunded(event);

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        NotificationRequestedEvent notification = captor.getValue();
        assertThat(notification.recipientUserId()).isEqualTo(shipment.getSender().getId());
        assertThat(notification.title()).isEqualTo("Payment Refunded");
        assertThat(notification.message()).contains("refunded successfully");

        verify(mailService).sendPaymentRefundedEmail(shipment.getSender().getEmail(), shipment.getId(), amount);
    }

    private Shipment shipment() {
        User sender = User.builder()
                .id(UUID.randomUUID())
                .email("sender@test.com")
                .firstName("Sender")
                .lastName("User")
                .build();

        return Shipment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .build();
    }
}
