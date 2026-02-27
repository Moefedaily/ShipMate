package com.shipmate.config.stripe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.service.payment.stripeEventVerifier.StripeEventVerifier;
import com.stripe.model.Event;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class StripeTestConfig {

    @Bean
    @Primary
    public StripeEventVerifier stripeEventVerifier(ObjectMapper objectMapper) {

        return (payload, signatureHeader) -> {
            try {
                return objectMapper.readValue(payload, Event.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid test payload", e);
            }
        };
    }
}
