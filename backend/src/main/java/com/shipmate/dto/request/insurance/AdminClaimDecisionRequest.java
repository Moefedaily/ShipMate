package com.shipmate.dto.request.insurance;

import com.shipmate.model.insuranceClaim.ClaimStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminClaimDecisionRequest {

    private ClaimStatus decision;

    private String adminNotes;
}