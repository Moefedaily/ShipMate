package com.shipmate.dto.response.insurance;

import com.shipmate.model.insuranceClaim.ClaimReason;
import com.shipmate.model.insuranceClaim.ClaimStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InsuranceClaimResponse {

    private UUID id;

    private UUID shipmentId;

    private UUID claimantId;

    private BigDecimal declaredValueSnapshot;

    private BigDecimal coverageAmount;

    private BigDecimal deductibleRate;

    private BigDecimal compensationAmount;

    private ClaimReason claimReason;

    private ClaimStatus claimStatus;

    private String description;

    private List<String> photos;

    private String adminNotes;

    private UUID adminUserId;

    private Instant createdAt;

    private Instant resolvedAt;
}