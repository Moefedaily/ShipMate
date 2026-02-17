package com.shipmate.unit.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.service.earning.DriverEarningService;
import com.shipmate.service.payment.PaymentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DriverEarningService driverEarningService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentService paymentService;

    @BeforeEach
    void setup() {
        paymentService = new PaymentService(
                paymentRepository,
                null, 
                null,
                null,
                driverEarningService,
                null,
                objectMapper
        );
    }

    @Test
    void handleCaptured_shouldSetCaptured_andCreateEarning_whenNotCaptured() {

        String intentId = "pi_test_123";

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .stripePaymentIntentId(intentId)
                .build();

        String payload = """
        {
          "data": {
            "object": {
              "id": "%s"
            }
          }
        }
        """.formatted(intentId);

        when(paymentRepository.findByStripePaymentIntentId(intentId))
                .thenReturn(Optional.of(payment));

        paymentService.handleCaptured(payload);

        assertThat(payment.getPaymentStatus())
                .isEqualTo(PaymentStatus.CAPTURED);

        verify(paymentRepository).save(payment);
        verify(driverEarningService).createIfAbsent(payment);
    }

    @Test
    void handleCaptured_shouldDoNothing_whenAlreadyCaptured() {

        String intentId = "pi_test_123";

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.CAPTURED)
                .stripePaymentIntentId(intentId)
                .build();

        String payload = """
        {
          "data": {
            "object": {
              "id": "%s"
            }
          }
        }
        """.formatted(intentId);

        when(paymentRepository.findByStripePaymentIntentId(intentId))
                .thenReturn(Optional.of(payment));

        paymentService.handleCaptured(payload);

        verify(paymentRepository, never()).save(any());
        verify(driverEarningService, never()).createIfAbsent(any());
    }

    @Test
    void handleRefunded_shouldSetRefunded_andCreateAdjustment() {

        String intentId = "pi_test_123";

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.CAPTURED)
                .stripePaymentIntentId(intentId)
                .build();

        String payload = """
        {
          "data": {
            "object": {
              "payment_intent": "%s"
            }
          }
        }
        """.formatted(intentId);

        when(paymentRepository.findByStripePaymentIntentId(intentId))
                .thenReturn(Optional.of(payment));

        paymentService.handleRefunded(payload);

        assertThat(payment.getPaymentStatus())
                .isEqualTo(PaymentStatus.REFUNDED);

        verify(paymentRepository).save(payment);
        verify(driverEarningService).createRefundAdjustment(payment);
    }

    @Test
    void handleRefunded_shouldDoNothing_whenAlreadyRefunded() {

        String intentId = "pi_test_123";

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.REFUNDED)
                .stripePaymentIntentId(intentId)
                .build();

        String payload = """
        {
          "data": {
            "object": {
              "payment_intent": "%s"
            }
          }
        }
        """.formatted(intentId);

        when(paymentRepository.findByStripePaymentIntentId(intentId))
                .thenReturn(Optional.of(payment));

        paymentService.handleRefunded(payload);

        verify(paymentRepository, never()).save(any());
        verify(driverEarningService, never()).createRefundAdjustment(any());
    }
}
