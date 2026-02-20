package com.shipmate.service.payment.stripeEventVerifier;

import com.stripe.model.Event;

public interface StripeEventVerifier {

    Event verify(String payload, String signature);
}
