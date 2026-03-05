package com.shipmate.service.admin;

import com.shipmate.dto.response.admin.AdminDashboardResponse;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.insuranceClaim.ClaimStatus;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.insurance.InsuranceClaimRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ShipmentRepository shipmentRepository;
    private final InsuranceClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardResponse getDashboardMetrics() {

        long totalUsers = userRepository.count();

        long totalDrivers =
                driverProfileRepository.countByStatus(DriverStatus.APPROVED);

        long activeShipments =
                shipmentRepository.countByStatusIn(
                        List.of(
                                ShipmentStatus.ASSIGNED,
                                ShipmentStatus.IN_TRANSIT
                        )
                );

        long completedShipments =
                shipmentRepository.countByStatus(ShipmentStatus.DELIVERED);

        long pendingClaims =
                claimRepository.countByClaimStatusIn(
                        List.of(
                                ClaimStatus.SUBMITTED,
                                ClaimStatus.UNDER_REVIEW
                        )
                );

        long totalPayments =
                paymentRepository.countByPaymentStatus(PaymentStatus.CAPTURED);

        var totalRevenue =
                paymentRepository.sumAmountByStatus(PaymentStatus.CAPTURED);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalDrivers(totalDrivers)
                .activeShipments(activeShipments)
                .completedShipments(completedShipments)
                .pendingClaims(pendingClaims)
                .totalPayments(totalPayments)
                .totalRevenue(totalRevenue)
                .build();
    }
}