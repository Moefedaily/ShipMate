package com.shipmate.dto.response.earning;

import com.shipmate.model.earning.PayoutStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DriverEarningResponse {

    private UUID id;

    private UUID driverId;

    private UUID shipmentId;

    private UUID paymentId;

    private BigDecimal grossAmount;

    private BigDecimal commissionAmount;

    private BigDecimal netAmount;

    private PayoutStatus payoutStatus;

    private Instant createdAt;
}
