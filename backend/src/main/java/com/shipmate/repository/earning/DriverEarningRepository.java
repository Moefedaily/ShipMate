package com.shipmate.repository.earning;

import com.shipmate.model.earning.DriverEarning;
import com.shipmate.model.earning.EarningType;
import com.shipmate.model.earning.PayoutStatus;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.user.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverEarningRepository extends JpaRepository<DriverEarning, UUID> {

    boolean existsByPaymentAndEarningType(Payment payment, EarningType earningType);

    Optional<DriverEarning> findByPaymentAndEarningType(Payment payment, EarningType earningType);

    List<DriverEarning> findAllByPayment(Payment payment);

    Page<DriverEarning> findByDriver(User driver, Pageable pageable);

    Optional<DriverEarning> findTopByDriverOrderByCreatedAtDesc(User driver);

    @Query("""
        SELECT COALESCE(SUM(e.grossAmount), 0)
        FROM DriverEarning e
        WHERE e.driver = :driver
        """)
    BigDecimal sumGrossByDriver(@Param("driver") User driver);

    @Query("""
        SELECT COALESCE(SUM(e.commissionAmount), 0)
        FROM DriverEarning e
        WHERE e.driver = :driver
        """)
    BigDecimal sumCommissionByDriver(@Param("driver") User driver);

    @Query("""
        SELECT COALESCE(SUM(e.netAmount), 0)
        FROM DriverEarning e
        WHERE e.driver = :driver
        """)
    BigDecimal sumNetByDriver(@Param("driver") User driver);

    @Query("""
        SELECT COALESCE(SUM(e.netAmount), 0)
        FROM DriverEarning e
        WHERE e.driver = :driver
        AND e.payoutStatus = :status
        """)
    BigDecimal sumNetByDriverAndStatus(
            @Param("driver") User driver,
            @Param("status") PayoutStatus status
    );
}
