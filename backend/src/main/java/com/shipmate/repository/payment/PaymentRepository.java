package com.shipmate.repository.payment;

import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByShipment(Shipment shipment);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    boolean existsByShipment(Shipment shipment);

    Page<Payment> findBySender(User sender, Pageable pageable);

    @Query("""
    SELECT COALESCE(SUM(p.amountTotal), 0)
    FROM Payment p
    WHERE p.paymentStatus = :status
    """)
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    long countByPaymentStatus(PaymentStatus status);
    long countBySender(User sender);
}
