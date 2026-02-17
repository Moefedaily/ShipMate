package com.shipmate.service.payment;

public interface PaymentGateway {

    void capture(String paymentIntentId);
}
