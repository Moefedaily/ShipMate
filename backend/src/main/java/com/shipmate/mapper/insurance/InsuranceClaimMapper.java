package com.shipmate.mapper.insurance;

import com.shipmate.dto.response.insurance.InsuranceClaimResponse;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InsuranceClaimMapper {

    @Mapping(target = "shipmentId", source = "shipment.id")
    @Mapping(target = "claimantId", source = "claimant.id")
    @Mapping(target = "adminUserId", source = "adminUser.id")
    @Mapping(target = "description", source = "claimDescription")
    InsuranceClaimResponse toResponse(InsuranceClaim claim);
}