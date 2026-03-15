package com.shipmate.dto.request.insurance;

import com.shipmate.model.insuranceClaim.ClaimReason;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInsuranceClaimRequest {

    private ClaimReason claimReason;

    private String description;
}
