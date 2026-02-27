package com.shipmate.repository.payment;

import com.shipmate.model.payment.Payment;
import com.shipmate.model.shipment.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByShipment(Shipment shipment);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    boolean existsByShipment(Shipment shipment);
}
