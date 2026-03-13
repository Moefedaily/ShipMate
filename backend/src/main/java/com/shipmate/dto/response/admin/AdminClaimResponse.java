package com.shipmate.dto.response.admin;

import com.shipmate.dto.response.photo.PhotoResponse;
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
public class AdminClaimResponse {

    private UUID id;

    private UUID shipmentId;

    private UUID claimantId;

    private ShipmentSummary shipment;

    private UserSummary sender;

    private UserSummary driver;

    private BigDecimal declaredValueSnapshot;

    private BigDecimal compensationAmount;

    private ClaimReason claimReason;

    private ClaimStatus claimStatus;

    private String description;

    private List<PhotoResponse> photos;

    private String adminNotes;

    private Instant createdAt;

    private Instant resolvedAt;
}