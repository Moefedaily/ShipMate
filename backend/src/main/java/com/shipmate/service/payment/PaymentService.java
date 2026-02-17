package com.shipmate.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.dto.response.payment.CreatePaymentIntentResponse;
import com.shipmate.dto.response.payment.PaymentResponse;
import com.shipmate.mapper.payment.PaymentMapper;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.earning.DriverEarningService;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final DriverEarningService driverEarningService;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;

    public CreatePaymentIntentResponse createPaymentIntent(UUID shipmentId, UUID senderId) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (!shipment.getSender().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("Not authorized for this shipment");
        }

        if (shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new IllegalStateException("Payment allowed only when shipment is ASSIGNED");
        }

        Payment payment = paymentRepository.findByShipment(shipment)
                .orElseGet(() -> createPaymentEntity(shipment));

        if (payment.getPaymentStatus() == PaymentStatus.AUTHORIZED ||
            payment.getPaymentStatus() == PaymentStatus.CAPTURED) {

            throw new IllegalStateException("Shipment already paid");
        }

        try {

            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(toStripeAmount(payment.getAmountTotal()))
                            .setCurrency(payment.getCurrency().toLowerCase())
                            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .build()
                            )
                            .putMetadata("shipmentId", shipment.getId().toString())
                            .putMetadata("senderId", sender.getId().toString())
                            .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());
            payment.setPaymentStatus(PaymentStatus.PROCESSING);

            paymentRepository.save(payment);

            return CreatePaymentIntentResponse.builder()
                    .clientSecret(intent.getClientSecret())
                    .paymentStatus(payment.getPaymentStatus())
                    .amountTotal(payment.getAmountTotal())
                    .currency(payment.getCurrency())
                    .build();

        } catch (Exception e) {
            log.error("Stripe payment intent creation failed", e);
            throw new RuntimeException("Unable to create payment intent");
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentForShipment(UUID shipmentId, UUID senderId) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (!shipment.getSender().getId().equals(senderId)) {
            throw new IllegalArgumentException("Not authorized");
        }

        Payment payment = paymentRepository.findByShipment(shipment)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        return paymentMapper.toResponse(payment);
    }

    private Payment createPaymentEntity(Shipment shipment) {

        BigDecimal total = shipment.getBasePrice()
                .add(shipment.getExtraInsuranceFee() != null
                        ? shipment.getExtraInsuranceFee()
                        : BigDecimal.ZERO);

        Payment payment = Payment.builder()
                .shipment(shipment)
                .sender(shipment.getSender())
                .amountTotal(total)
                .currency("EUR")
                .paymentStatus(PaymentStatus.REQUIRED)
                .build();

        return paymentRepository.save(payment);
    }

    private Long toStripeAmount(BigDecimal amount) {

        BigDecimal normalized = amount
                .setScale(2, RoundingMode.HALF_UP);

        return normalized
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public void handleAuthorized(Event event) {

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer == null) return;


        if (deserializer.getObject().isEmpty()) return;

        PaymentIntent intent = (PaymentIntent) deserializer.getObject().get();

        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .ifPresent(payment -> {

                    if (payment.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
                        return;
                    }

                    payment.setPaymentStatus(PaymentStatus.AUTHORIZED);
                    paymentRepository.save(payment);
                });
    }

    public void handlePaymentFailed(Event event) {

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer == null) return;


        if (deserializer.getObject().isEmpty()) return;

        PaymentIntent intent = (PaymentIntent) deserializer.getObject().get();

        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .ifPresent(payment -> {

                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(intent.getLastPaymentError() != null
                            ? intent.getLastPaymentError().getMessage()
                            : "Payment failed");

                    paymentRepository.save(payment);
                });
    }

   public void handleCaptured(String payload) {

    try {

        JsonNode root = objectMapper.readTree(payload);

        String paymentIntentId = root
                .path("data")
                .path("object")
                .path("id")
                .asText();

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return;
        }

        paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .ifPresent(payment -> {

                    if (payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
                        return;
                    }

                    payment.setPaymentStatus(PaymentStatus.CAPTURED);
                    paymentRepository.save(payment);

                    driverEarningService.createIfAbsent(payment);
                });

    } catch (Exception e) {
        throw new IllegalStateException("Failed to process capture event", e);
    }
}



  public void handleRefunded(String payload) {

    try {

        JsonNode root = objectMapper.readTree(payload);

        String paymentIntentId = root
                .path("data")
                .path("object")
                .path("payment_intent")
                .asText();

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return;
        }

        paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .ifPresent(payment -> {

                    if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
                        return;
                    }

                    payment.setPaymentStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);

                    driverEarningService.createRefundAdjustment(payment);
                });

    } catch (Exception e) {
        throw new IllegalStateException("Refund handling failed", e);
    }
}






    public void capturePaymentForShipment(Shipment shipment) {

        Payment payment = paymentRepository.findByShipment(shipment)
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        if (payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
            return; // idempotent
        }

        if (payment.getPaymentStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment not authorized for capture");
        }

        paymentGateway.capture(payment.getStripePaymentIntentId());
        log.info(
                "[PAYMENT] capture requested shipmentId={} paymentId={}",
                shipment.getId(),
                payment.getId()
        );
    }


    public void handleCancellation(Shipment shipment) {

        paymentRepository.findByShipment(shipment)
                .ifPresent(payment -> {

                    switch (payment.getPaymentStatus()) {

                        case REQUIRED -> {
                            payment.setPaymentStatus(PaymentStatus.CANCELLED);
                            paymentRepository.save(payment);
                        }

                        case PROCESSING -> {
                            payment.setPaymentStatus(PaymentStatus.CANCELLED);
                            paymentRepository.save(payment);
                        }

                        case AUTHORIZED -> {
                            cancelAuthorization(payment);
                        }

                        case CAPTURED -> {
                            refundPayment(payment);
                        }

                        default -> {
                        }
                    }
                });
    }

    private void cancelAuthorization(Payment payment) {

        try {

            PaymentIntent intent = PaymentIntent.retrieve(
                    payment.getStripePaymentIntentId()
            );

            intent.cancel();

            // Webhook will update status to CANCELLED
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel authorization", e);
        }
    }

    private void refundPayment(Payment payment) {

        try {

            RefundCreateParams params =
                    RefundCreateParams.builder()
                            .setPaymentIntent(payment.getStripePaymentIntentId())
                            .build();

            Refund.create(params);
            log.info(
                    "[PAYMENT] refund requested paymentId={}",
                    payment.getId()
            );

        } catch (Exception e) {
            throw new RuntimeException("Refund failed", e);
        }
    }

}
