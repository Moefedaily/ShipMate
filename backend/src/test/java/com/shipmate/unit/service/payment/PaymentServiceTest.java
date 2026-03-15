package com.shipmate.unit.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.dto.response.payment.PaymentResponse;
import com.shipmate.listener.delivery.DeliveryCodeEventPublisher;
import com.shipmate.listener.payment.PaymentCapturedEvent;
import com.shipmate.listener.payment.PaymentRefundedEvent;
import com.shipmate.mapper.payment.PaymentMapper;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.admin.AdminActionLogger;
import com.shipmate.service.delivery.DeliveryCodeService;
import com.shipmate.service.earning.DriverEarningService;
import com.shipmate.service.payment.PaymentGateway;
import com.shipmate.service.payment.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private DriverEarningService driverEarningService;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private DeliveryCodeService deliveryCodeService;

    @Mock
    private DeliveryCodeEventPublisher deliveryCodeEventPublisher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AdminActionLogger adminActionLogger;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                shipmentRepository,
                userRepository,
                paymentMapper,
                driverEarningService,
                paymentGateway,
                new ObjectMapper(),
                deliveryCodeService,
                deliveryCodeEventPublisher,
                eventPublisher,
                adminActionLogger
        );
    }

    @Test
    void getPaymentForShipment_shouldReturnMappedPaymentWhenPersisted() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        Payment payment = payment(shipment, PaymentStatus.PROCESSING);
        PaymentResponse response = PaymentResponse.builder()
                .shipmentId(shipment.getId())
                .paymentStatus(PaymentStatus.PROCESSING)
                .amountTotal(payment.getAmountTotal())
                .currency(payment.getCurrency())
                .build();

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.getPaymentForShipment(shipment.getId(), shipment.getSender().getId());

        assertThat(result).isSameAs(response);
    }

    @Test
    void getPaymentForShipment_shouldBuildRequiredResponseWhenMissingPayment() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        shipment.setBasePrice(new BigDecimal("100.00"));
        shipment.setInsuranceFee(new BigDecimal("10.00"));

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.empty());

        PaymentResponse result = paymentService.getPaymentForShipment(shipment.getId(), shipment.getSender().getId());

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REQUIRED);
        assertThat(result.getAmountTotal()).isEqualByComparingTo("110.00");
        assertThat(result.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void getPaymentForShipment_shouldRejectWrongSender() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> paymentService.getPaymentForShipment(shipment.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not authorized");
    }

    @Test
    void handleAuthorized_shouldMoveProcessingPaymentToAuthorizedAndGenerateCode() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        Payment payment = payment(shipment, PaymentStatus.PROCESSING);
        payment.setStripePaymentIntentId("pi_auth");

        when(paymentRepository.findByStripePaymentIntentId("pi_auth")).thenReturn(Optional.of(payment));
        when(deliveryCodeService.isVerified(shipment)).thenReturn(false);
        when(deliveryCodeService.generateAndStore(shipment)).thenReturn("123456");

        paymentService.handleAuthorized("""
        {
          "data": {
            "object": {
              "id": "pi_auth",
              "status": "requires_capture"
            }
          }
        }
        """);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        verify(paymentRepository).save(payment);
        verify(deliveryCodeEventPublisher).publishToSender(shipment.getSender().getId(), shipment.getId(), "123456");
    }

    @Test
    void handleAuthorized_shouldIgnoreInvalidShipmentStatus() {
        Shipment shipment = shipment(ShipmentStatus.CREATED);
        Payment payment = payment(shipment, PaymentStatus.PROCESSING);
        payment.setStripePaymentIntentId("pi_auth");

        when(paymentRepository.findByStripePaymentIntentId("pi_auth")).thenReturn(Optional.of(payment));

        paymentService.handleAuthorized("""
        {
          "data": {
            "object": {
              "id": "pi_auth",
              "status": "requires_capture"
            }
          }
        }
        """);

        verify(paymentRepository, never()).save(any());
        verify(deliveryCodeService, never()).generateAndStore(any());
    }

    @Test
    void handleCaptured_shouldCaptureDeliveredAuthorizedPaymentAndPublishEvent() {
        Shipment shipment = shipment(ShipmentStatus.DELIVERED);
        Payment payment = payment(shipment, PaymentStatus.AUTHORIZED);
        payment.setStripePaymentIntentId("pi_cap");

        when(paymentRepository.findByStripePaymentIntentId("pi_cap")).thenReturn(Optional.of(payment));

        paymentService.handleCaptured("""
        {
          "data": {
            "object": {
              "id": "pi_cap"
            }
          }
        }
        """);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(paymentRepository).save(payment);
        verify(driverEarningService).createIfAbsent(payment);

        ArgumentCaptor<PaymentCapturedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCapturedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().shipmentId()).isEqualTo(shipment.getId());
    }

    @Test
    void handleCaptured_shouldIgnoreNonDeliveredShipment() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        Payment payment = payment(shipment, PaymentStatus.AUTHORIZED);
        payment.setStripePaymentIntentId("pi_cap");

        when(paymentRepository.findByStripePaymentIntentId("pi_cap")).thenReturn(Optional.of(payment));

        paymentService.handleCaptured("""
        {
          "data": {
            "object": {
              "id": "pi_cap"
            }
          }
        }
        """);

        verify(paymentRepository, never()).save(any());
        verify(driverEarningService, never()).createIfAbsent(any());
    }

    @Test
    void handleRefunded_shouldRefundCapturedPaymentAndCreateAdjustment() {
        Shipment shipment = shipment(ShipmentStatus.DELIVERED);
        Payment payment = payment(shipment, PaymentStatus.CAPTURED);
        payment.setStripePaymentIntentId("pi_ref");

        when(paymentRepository.findByStripePaymentIntentId("pi_ref")).thenReturn(Optional.of(payment));

        paymentService.handleRefunded("""
        {
          "data": {
            "object": {
              "payment_intent": "pi_ref"
            }
          }
        }
        """);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
        verify(driverEarningService).createRefundAdjustment(payment);

        ArgumentCaptor<PaymentRefundedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().shipmentId()).isEqualTo(shipment.getId());
    }

    @Test
    void handleRefunded_shouldIgnoreAlreadyRefundedPayment() {
        Shipment shipment = shipment(ShipmentStatus.DELIVERED);
        Payment payment = payment(shipment, PaymentStatus.REFUNDED);
        payment.setStripePaymentIntentId("pi_ref");

        when(paymentRepository.findByStripePaymentIntentId("pi_ref")).thenReturn(Optional.of(payment));

        paymentService.handleRefunded("""
        {
          "data": {
            "object": {
              "payment_intent": "pi_ref"
            }
          }
        }
        """);

        verify(paymentRepository, never()).save(any());
        verify(driverEarningService, never()).createRefundAdjustment(any());
    }

    @Test
    void handleStripeCanceled_shouldCancelAuthorizedPayment() {
        Shipment shipment = shipment(ShipmentStatus.CANCELLED);
        Payment payment = payment(shipment, PaymentStatus.AUTHORIZED);
        payment.setStripePaymentIntentId("pi_cancel");

        when(paymentRepository.findByStripePaymentIntentId("pi_cancel")).thenReturn(Optional.of(payment));

        paymentService.handleStripeCanceled("""
        {
          "data": {
            "object": {
              "id": "pi_cancel"
            }
          }
        }
        """);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleCancellation_shouldCancelRequiredPaymentLocally() {
        Shipment shipment = shipment(ShipmentStatus.CANCELLED);
        Payment payment = payment(shipment, PaymentStatus.REQUIRED);

        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.of(payment));

        paymentService.handleCancellation(shipment);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleCancellation_shouldCancelFailedPaymentLocally() {
        Shipment shipment = shipment(ShipmentStatus.CANCELLED);
        Payment payment = payment(shipment, PaymentStatus.FAILED);

        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.of(payment));

        paymentService.handleCancellation(shipment);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void capturePaymentForShipment_shouldInvokeGatewayForAuthorizedPayment() {
        Shipment shipment = shipment(ShipmentStatus.DELIVERED);
        Payment payment = payment(shipment, PaymentStatus.AUTHORIZED);
        payment.setStripePaymentIntentId("pi_cap");

        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.of(payment));

        paymentService.capturePaymentForShipment(shipment);

        verify(paymentGateway).capture("pi_cap");
    }

    @Test
    void capturePaymentForShipment_shouldRejectNonAuthorizedPayment() {
        Shipment shipment = shipment(ShipmentStatus.DELIVERED);
        Payment payment = payment(shipment, PaymentStatus.PROCESSING);

        when(paymentRepository.findByShipment(shipment)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.capturePaymentForShipment(shipment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment not authorized for capture");
    }

    @Test
    void refundByAdmin_shouldRejectNonCapturedPayment() {
        Shipment shipment = shipment(ShipmentStatus.DELIVERED);
        Payment payment = payment(shipment, PaymentStatus.AUTHORIZED);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundByAdmin(payment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only captured payments can be refunded");
    }

    @Test
    void handlePaymentFailed_shouldMarkProcessingPaymentAsFailed() {
        Shipment shipment = shipment(ShipmentStatus.ASSIGNED);
        Payment payment = payment(shipment, PaymentStatus.PROCESSING);
        payment.setStripePaymentIntentId("pi_fail");

        Event event = org.mockito.Mockito.mock(Event.class);
        EventDataObjectDeserializer deserializer = org.mockito.Mockito.mock(EventDataObjectDeserializer.class);
        PaymentIntent intent = org.mockito.Mockito.mock(PaymentIntent.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(intent));
        when(intent.getId()).thenReturn("pi_fail");
        when(intent.getLastPaymentError()).thenReturn(null);
        when(paymentRepository.findByStripePaymentIntentId("pi_fail")).thenReturn(Optional.of(payment));

        paymentService.handlePaymentFailed(event);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Payment failed");
        verify(paymentRepository).save(payment);
    }

    private Shipment shipment(ShipmentStatus status) {
        User sender = User.builder()
                .id(UUID.randomUUID())
                .email("sender@test.com")
                .build();

        return Shipment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .status(status)
                .basePrice(new BigDecimal("75.00"))
                .insuranceFee(new BigDecimal("5.00"))
                .updatedAt(Instant.now())
                .deliveredAt(Instant.now())
                .build();
    }

    private Payment payment(Shipment shipment, PaymentStatus status) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .sender(shipment.getSender())
                .amountTotal(new BigDecimal("80.00"))
                .currency("EUR")
                .paymentStatus(status)
                .build();
    }
}
