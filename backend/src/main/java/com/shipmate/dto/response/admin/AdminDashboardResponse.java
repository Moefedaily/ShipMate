package com.shipmate.dto.response.admin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminDashboardResponse {

    private long totalUsers;

    private long totalDrivers;

    private long activeShipments;

    private long completedShipments;

    private long pendingClaims;

    private long totalPayments;

    private BigDecimal totalRevenue;

    private long pendingApprovals;
}