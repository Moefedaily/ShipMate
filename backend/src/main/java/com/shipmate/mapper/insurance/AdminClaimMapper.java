package com.shipmate.mapper.insurance;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.shipmate.dto.response.admin.AdminClaimResponse;
import com.shipmate.dto.response.admin.ShipmentSummary;
import com.shipmate.dto.response.admin.UserSummary;
import com.shipmate.mapper.photo.PhotoMapper;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;

@Mapper(componentModel = "spring", uses = {PhotoMapper.class})
public interface AdminClaimMapper {

    @Mapping(target = "shipment", source = "shipment")
    @Mapping(target = "sender", source = "shipment.sender")
    @Mapping(target = "driver", source = "shipment.booking.driver")
    @Mapping(target = "description", source = "claimDescription")
    AdminClaimResponse toAdminResponse(InsuranceClaim claim);


    ShipmentSummary toShipmentSummary(Shipment shipment);

    UserSummary toUserSummary(User user);

}