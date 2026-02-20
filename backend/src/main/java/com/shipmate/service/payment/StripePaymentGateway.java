package com.shipmate.service.payment;

import com.stripe.model.PaymentIntent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public void capture(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent.capture();
        } catch (Exception e) {
            throw new IllegalStateException("Stripe capture failed", e);
        }
    }
}
