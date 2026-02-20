package com.shipmate.service.payment.stripeEventVerifier;

import com.stripe.model.Event;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class StripeSdkEventVerifier implements StripeEventVerifier {

    @Value("${stripe.webhook-secret}")
    private String endpointSecret;

    @Override
    public Event verify(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, endpointSecret);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid signature", e);
        }
    }
}
