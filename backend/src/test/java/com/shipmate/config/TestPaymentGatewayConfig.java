package com.shipmate.config;

import com.shipmate.service.payment.PaymentGateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestPaymentGatewayConfig {

    @Bean
    @Primary
    public PaymentGateway testPaymentGateway() {
        return paymentIntentId -> {
            // no-op
        };
    }
}
