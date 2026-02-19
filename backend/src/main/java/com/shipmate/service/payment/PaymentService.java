package com.shipmate.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.dto.response.payment.CreatePaymentIntentResponse;
import com.shipmate.dto.response.payment.PaymentResponse;
import com.shipmate.listener.delivery.DeliveryCodeEventPublisher;
import com.shipmate.mapper.payment.PaymentMapper;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.delivery.DeliveryCodeService;
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
    private final DeliveryCodeService deliveryCodeService;
    private final DeliveryCodeEventPublisher deliveryCodeEventPublisher;

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

        // Already paid (terminal for pay action)
        if (payment.getPaymentStatus() == PaymentStatus.AUTHORIZED ||
            payment.getPaymentStatus() == PaymentStatus.CAPTURED) {

            throw new IllegalStateException("Shipment already paid");
        }

        // If user already started payment and i have an existing intent -> reuse it
        if (payment.getPaymentStatus() == PaymentStatus.PROCESSING &&
            payment.getStripePaymentIntentId() != null &&
            !payment.getStripePaymentIntentId().isBlank()) {

            try {

                PaymentIntent existing = PaymentIntent.retrieve(
                        payment.getStripePaymentIntentId()
                );

                String clientSecret = existing.getClientSecret();

                if (clientSecret == null || clientSecret.isBlank()) {
                    log.warn("[PAYMENT] Existing intent missing clientSecret paymentId={} intentId={}",
                            payment.getId(),
                            payment.getStripePaymentIntentId());
                    throw new IllegalStateException("Existing payment intent not usable");
                }

                return CreatePaymentIntentResponse.builder()
                        .clientSecret(clientSecret)
                        .paymentStatus(payment.getPaymentStatus())
                        .amountTotal(payment.getAmountTotal())
                        .currency(payment.getCurrency())
                        .build();

            } catch (Exception e) {
                log.error("[PAYMENT] Failed to retrieve existing intent, will create a new one paymentId={}",
                        payment.getId(), e);
                // fallthrough to create a new intent below
            }
        }

        // If previously FAILED or REQUIRED, create a new attempt
        if (payment.getPaymentStatus() != PaymentStatus.REQUIRED &&
            payment.getPaymentStatus() != PaymentStatus.FAILED &&
            payment.getPaymentStatus() != PaymentStatus.PROCESSING) {

            throw new IllegalStateException("Invalid payment state for intent creation: " + payment.getPaymentStatus());
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
            payment.setFailureReason(null); // reset previous failure

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

        return paymentRepository.findByShipment(shipment)
                .map(paymentMapper::toResponse)
                .orElse(
                    PaymentResponse.builder()
                        .shipmentId(shipment.getId())
                        .paymentStatus(PaymentStatus.REQUIRED)
                        .amountTotal(
                            shipment.getBasePrice()
                                .add(
                                    shipment.getExtraInsuranceFee() != null
                                        ? shipment.getExtraInsuranceFee()
                                        : BigDecimal.ZERO
                                )
                        )
                        .currency("EUR")
                        .build()
                );

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

   @Transactional
    public void handleAuthorized(String payload) {

        try {

            JsonNode root = objectMapper.readTree(payload);
            JsonNode objectNode = root.path("data").path("object");

            String paymentIntentId = objectNode.path("id").asText(null);
            String status = objectNode.path("status").asText(null);

            if (paymentIntentId == null || status == null) {
                log.warn("[PAYMENT] Invalid authorization payload");
                return;
            }

            if (!"requires_capture".equals(status)) {
                return;
            }

            paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                    .ifPresent(payment -> {

                        Shipment shipment = payment.getShipment();

                        // Terminal guard
                        if (isPaymentTerminal(payment.getPaymentStatus())) {
                            log.warn("[PAYMENT][WEBHOOK] Ignored AUTHORIZED - payment terminal status={}",
                                    payment.getPaymentStatus());
                            return;
                        }

                        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
                            log.warn("[PAYMENT][WEBHOOK] Ignored AUTHORIZED - shipment CANCELLED shipmentId={}",
                                    shipment.getId());
                            return;
                        }

                        // Shipment state must be ASSIGNED
                        if (shipment.getStatus() != ShipmentStatus.ASSIGNED) {
                            log.warn("[PAYMENT][WEBHOOK] Ignored AUTHORIZED - shipment status={} shipmentId={}",
                                    shipment.getStatus(),
                                    shipment.getId());
                            return;
                        }

                        // Transition guard
                        if (payment.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
                            return;
                        }

                        if (!isValidTransition(payment.getPaymentStatus(), PaymentStatus.AUTHORIZED)) {
                            log.warn("[PAYMENT][WEBHOOK] Invalid transition {} -> AUTHORIZED paymentId={}",
                                    payment.getPaymentStatus(),
                                    payment.getId());
                            return;
                        }

                        // Apply transition
                        payment.setPaymentStatus(PaymentStatus.AUTHORIZED);
                        paymentRepository.save(payment);

                        log.info("[PAYMENT] Authorized paymentIntentId={}", paymentIntentId);

                        // Generate delivery code safely
                        if (!deliveryCodeService.isVerified(shipment)) {

                            String code = deliveryCodeService.generateAndStore(shipment);

                            if (code != null) {

                                deliveryCodeEventPublisher.publishToSender(
                                        shipment.getSender().getId(),
                                        shipment.getId(),
                                        code
                                );

                                log.info("[DELIVERY_CODE] Generated & pushed shipmentId={}",
                                        shipment.getId());
                            }
                        }
                    });

        } catch (Exception e) {
            log.error("[PAYMENT] Failed to process authorization event", e);
            throw new IllegalStateException("Failed to process authorization event", e);
        }
    }


    public void handlePaymentFailed(Event event) {

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer == null) return;

        if (deserializer.getObject().isEmpty()) return;

        PaymentIntent intent = (PaymentIntent) deserializer.getObject().get();

        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .ifPresent(payment -> {

                    if (isPaymentTerminal(payment.getPaymentStatus())) {
                        log.warn("[PAYMENT][WEBHOOK] Ignored FAILED - terminal status={}",
                                payment.getPaymentStatus());
                        return;
                    }

                    if (!isValidTransition(payment.getPaymentStatus(), PaymentStatus.FAILED)) {
                        log.warn("[PAYMENT][WEBHOOK] Invalid transition {} -> FAILED paymentId={}",
                                payment.getPaymentStatus(),
                                payment.getId());
                        return;
                    }

                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(
                            intent.getLastPaymentError() != null
                                    ? intent.getLastPaymentError().getMessage()
                                    : "Payment failed"
                    );

                    paymentRepository.save(payment);
                });
    }


    @Transactional
    public void handleCaptured(String payload) {

        try {

            JsonNode root = objectMapper.readTree(payload);

            String paymentIntentId = root
                    .path("data")
                    .path("object")
                    .path("id")
                    .asText(null);

            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                log.warn("[PAYMENT] Capture event missing paymentIntentId");
                return;
            }

            paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                    .ifPresent(payment -> {

                        Shipment shipment = payment.getShipment();

                        // Terminal guard
                        if (isPaymentTerminal(payment.getPaymentStatus())) {
                            log.warn("[PAYMENT][WEBHOOK] Ignored CAPTURED - payment terminal status={}",
                                    payment.getPaymentStatus());
                            return;
                        }

                        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
                            log.warn("[PAYMENT][WEBHOOK] Ignored CAPTURED - shipment CANCELLED shipmentId={}",
                                    shipment.getId());
                            return;
                        }

                        // Shipment must be DELIVERED
                        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
                            log.warn("[PAYMENT][WEBHOOK] Ignored CAPTURED - shipment not DELIVERED shipmentId={} status={}",
                                    shipment.getId(),
                                    shipment.getStatus());
                            return;
                        }

                        // Transition guard
                        if (payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
                            return; // idempotent
                        }

                        if (!isValidTransition(payment.getPaymentStatus(), PaymentStatus.CAPTURED)) {
                            log.warn("[PAYMENT][WEBHOOK] Invalid transition {} -> CAPTURED paymentId={}",
                                    payment.getPaymentStatus(),
                                    payment.getId());
                            return;
                        }

                        // Apply transition
                        payment.setPaymentStatus(PaymentStatus.CAPTURED);
                        paymentRepository.save(payment);

                        driverEarningService.createIfAbsent(payment);

                        log.info("[PAYMENT] Captured paymentIntentId={}", paymentIntentId);
                    });

        } catch (Exception e) {
            log.error("[PAYMENT] Failed to process capture event", e);
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
                    .asText(null);

            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                return;
            }

            paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                    .ifPresent(payment -> {

                        // Terminal guard
                        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
                            return;
                        }

                        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
                            log.warn("[PAYMENT][WEBHOOK] Ignored REFUND - payment CANCELLED paymentId={}",
                                    payment.getId());
                            return;
                        }

                        // Transition guard
                        if (!isValidTransition(payment.getPaymentStatus(), PaymentStatus.REFUNDED)) {
                            log.warn("[PAYMENT][WEBHOOK] Invalid transition {} -> REFUNDED paymentId={}",
                                    payment.getPaymentStatus(),
                                    payment.getId());
                            return;
                        }

                        // Apply transition
                        payment.setPaymentStatus(PaymentStatus.REFUNDED);
                        paymentRepository.save(payment);

                        driverEarningService.createRefundAdjustment(payment);

                        log.info("[PAYMENT] Refunded paymentIntentId={}", paymentIntentId);
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

                    PaymentStatus current = payment.getPaymentStatus();

                    switch (current) {

                        case REQUIRED -> {
                            payment.setPaymentStatus(PaymentStatus.CANCELLED);
                            paymentRepository.save(payment);
                        }

                        case PROCESSING -> {
                            cancelStripeIntentIfExists(payment);
                            payment.setPaymentStatus(PaymentStatus.CANCELLED);
                            paymentRepository.save(payment);
                        }

                        case AUTHORIZED -> {
                            cancelStripeIntentIfExists(payment);
                            payment.setPaymentStatus(PaymentStatus.CANCELLED);
                            paymentRepository.save(payment);
                        }

                        case CAPTURED -> {
                            refundPayment(payment);
                        }

                        case FAILED -> {
                            payment.setPaymentStatus(PaymentStatus.CANCELLED);
                            paymentRepository.save(payment);
                        }

                        default -> {
                            // CANCELLED / REFUNDED → nothing
                        }
                    }

                    log.info("[PAYMENT] Cancellation handled shipmentId={} paymentStatus={}",
                            shipment.getId(),
                            payment.getPaymentStatus());
                });
    }

    @Transactional
    public void handleStripeCanceled(String payload) {

        try {

            JsonNode root = objectMapper.readTree(payload);

            String paymentIntentId = root
                    .path("data")
                    .path("object")
                    .path("id")
                    .asText(null);

            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                return;
            }

            paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                    .ifPresent(payment -> {

                        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
                            return; // idempotent
                        }

                        if (!isValidTransition(payment.getPaymentStatus(), PaymentStatus.CANCELLED)) {
                            log.warn("[PAYMENT][WEBHOOK] Invalid transition {} -> CANCELLED paymentId={}",
                                    payment.getPaymentStatus(),
                                    payment.getId());
                            return;
                        }

                        payment.setPaymentStatus(PaymentStatus.CANCELLED);
                        paymentRepository.save(payment);

                        log.info("[PAYMENT] Stripe confirmed cancellation intentId={}",
                                paymentIntentId);
                    });

        } catch (Exception e) {
            log.error("[PAYMENT] Failed to process Stripe cancel event", e);
            throw new IllegalStateException("Cancel webhook failed", e);
        }
    }

    private void cancelStripeIntentIfExists(Payment payment) {

        if (payment.getStripePaymentIntentId() == null ||
            payment.getStripePaymentIntentId().isBlank()) {
            return;
        }

        try {

            PaymentIntent intent = PaymentIntent.retrieve(
                    payment.getStripePaymentIntentId()
            );

            intent.cancel();

            log.info("[PAYMENT] Stripe intent cancelled intentId={}",
                    payment.getStripePaymentIntentId());

        } catch (Exception e) {

            log.error("[PAYMENT] Failed to cancel Stripe intent intentId={}",
                    payment.getStripePaymentIntentId(), e);
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

    private boolean isPaymentTerminal(PaymentStatus status) {
        return status == PaymentStatus.CANCELLED ||
            status == PaymentStatus.REFUNDED;
    }

    private boolean isValidTransition(PaymentStatus from, PaymentStatus to) {

        return switch (from) {

            case REQUIRED -> to == PaymentStatus.PROCESSING;

            case PROCESSING ->
                    to == PaymentStatus.AUTHORIZED ||
                    to == PaymentStatus.FAILED ||
                    to == PaymentStatus.CANCELLED;

            case AUTHORIZED ->
                    to == PaymentStatus.CAPTURED ||
                    to == PaymentStatus.CANCELLED;

            case CAPTURED ->
                    to == PaymentStatus.REFUNDED;

            case FAILED ->
                    to == PaymentStatus.PROCESSING;

            case CANCELLED, REFUNDED -> false;
        };
    }

}
