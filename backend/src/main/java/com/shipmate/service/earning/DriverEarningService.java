package com.shipmate.service.earning;

import com.shipmate.dto.response.earning.DriverEarningResponse;
import com.shipmate.dto.response.earning.DriverEarningsSummaryResponse;
import com.shipmate.mapper.earning.DriverEarningMapper;
import com.shipmate.model.earning.DriverEarning;
import com.shipmate.model.earning.EarningType;
import com.shipmate.model.earning.PayoutStatus;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import com.shipmate.repository.earning.DriverEarningRepository;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DriverEarningService {

    private final DriverEarningRepository driverEarningRepository;
    private final UserRepository userRepository;
    private final DriverEarningMapper driverEarningMapper;

    @Value("${app.commission.rate:0.20}")
    private BigDecimal commissionRate;

    public void createIfAbsent(Payment payment) {

        Shipment shipment = payment.getShipment();

        if (shipment.getBooking() == null || shipment.getBooking().getDriver() == null) {
            throw new IllegalStateException("Shipment has no assigned driver");
        }

        if (driverEarningRepository
                .existsByPaymentAndEarningType(payment, EarningType.ORIGINAL)) {
            return;
        }

        BigDecimal gross = payment.getAmountTotal().setScale(2, RoundingMode.HALF_UP);

        BigDecimal commission = gross
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal net = gross
                .subtract(commission)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        DriverEarning earning = DriverEarning.builder()
                .driver(shipment.getBooking().getDriver())
                .shipment(shipment)
                .payment(payment)
                .earningType(EarningType.ORIGINAL)
                .grossAmount(gross)
                .commissionAmount(commission)
                .netAmount(net)
                .payoutStatus(PayoutStatus.PENDING)
                .build();

        driverEarningRepository.save(earning);

        log.info(
                "[EARNINGS] ORIGINAL created shipmentId={} paymentId={} net={}",
                shipment.getId(),
                payment.getId(),
                net
        );
    }

    @Transactional(readOnly = true)
    public Page<DriverEarningResponse> getMyEarnings(UUID driverId, Pageable pageable) {

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        return driverEarningRepository
                .findByDriver(driver, pageable)
                .map(driverEarningMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DriverEarningsSummaryResponse getMyEarningsSummary(UUID driverId) {

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        BigDecimal totalGross = driverEarningRepository.sumGrossByDriver(driver);

        BigDecimal totalCommission = driverEarningRepository.sumCommissionByDriver(driver);

        BigDecimal totalNet = driverEarningRepository.sumNetByDriver(driver);

        BigDecimal totalPending = driverEarningRepository.sumNetByDriverAndStatus(driver, PayoutStatus.PENDING);

        BigDecimal totalPaid = driverEarningRepository.sumNetByDriverAndStatus(driver, PayoutStatus.PAID);

        return DriverEarningsSummaryResponse.builder()
                .totalGross(totalGross)
                .totalCommission(totalCommission)
                .totalNet(totalNet)
                .totalPending(totalPending)
                .totalPaid(totalPaid)
                .build();
    }

    public void createRefundAdjustment(Payment payment) {

        Shipment shipment = payment.getShipment();

        var originalOpt = driverEarningRepository
                .findByPaymentAndEarningType(payment, EarningType.ORIGINAL);

        if (originalOpt.isEmpty()) {
            return; 
        }

        if (driverEarningRepository
                .existsByPaymentAndEarningType(payment, EarningType.REFUND)) {
            return;
        }

        DriverEarning original = originalOpt.get();

        DriverEarning adjustment = DriverEarning.builder()
                .driver(original.getDriver())
                .shipment(shipment)
                .payment(payment)
                .earningType(EarningType.REFUND)
                .grossAmount(original.getGrossAmount().negate())
                .commissionAmount(original.getCommissionAmount().negate())
                .netAmount(original.getNetAmount().negate())
                .payoutStatus(PayoutStatus.PENDING)
                .build();

        driverEarningRepository.save(adjustment);

        log.info(
                "[EARNINGS] REFUND created shipmentId={} net={}",
                shipment.getId(),
                adjustment.getNetAmount()
        );
    }

    @Transactional
    public void markAsPaid(UUID earningId) {

        DriverEarning earning = driverEarningRepository.findById(earningId)
                .orElseThrow(() -> new IllegalArgumentException("Earning not found"));

        if (earning.getPayoutStatus() == PayoutStatus.PAID) {
            return;
        }

        if (earning.getEarningType() != EarningType.ORIGINAL) {
            throw new IllegalStateException("Only ORIGINAL earnings can be paid");
        }

        earning.setPayoutStatus(PayoutStatus.PAID);

        driverEarningRepository.save(earning);
    }

    @Transactional
    public void markMultipleAsPaid(List<UUID> earningIds) {

        for (UUID id : earningIds) {
            markAsPaid(id);
        }
    }
}
