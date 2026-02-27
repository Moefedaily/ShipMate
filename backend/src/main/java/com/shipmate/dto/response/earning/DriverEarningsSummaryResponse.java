package com.shipmate.dto.response.earning;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DriverEarningsSummaryResponse {

    private BigDecimal totalGross;

    private BigDecimal totalCommission;

    private BigDecimal totalNet;

    private BigDecimal totalPending;

    private BigDecimal totalPaid;
}
