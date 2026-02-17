package com.shipmate.controller.webhook;

import com.shipmate.service.payment.PaymentService;
import com.shipmate.service.payment.stripeEventVerifier.StripeEventVerifier;
import com.stripe.model.Event;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final StripeEventVerifier stripeEventVerifier;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request)
            throws IOException {

        String payload = new String(
                request.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        String sigHeader = request.getHeader("Stripe-Signature");

        if (sigHeader == null) {
            return ResponseEntity.badRequest().body("Missing Stripe-Signature header");
        }

        Event event;

        try {
            event = stripeEventVerifier.verify(payload, sigHeader);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        handleEvent(event, payload);

        return ResponseEntity.ok("Webhook received");
    }

    private void handleEvent(Event event, String payload) {
        String eventType = event.getType();

        switch (eventType) {

            case "payment_intent.amount_capturable_updated" ->
                    paymentService.handleAuthorized(event);

            case "payment_intent.payment_failed" ->
                    paymentService.handlePaymentFailed(event);

            case "payment_intent.succeeded" ->
                    paymentService.handleCaptured(payload);

            case "charge.refunded" ->
                    paymentService.handleRefunded(payload);

            default -> {
                // ignore other events
            }
        }
    }
}
