package com.shipmate.mapper.shipment;

import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.model.shipment.Shipment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "pickupOrder", ignore = true)
    @Mapping(target = "deliveryOrder", ignore = true)
    @Mapping(target = "basePrice", ignore = true)
    @Mapping(target = "insuranceFee", ignore = true)
    @Mapping(target = "insuranceCoverageAmount", ignore = true)
    @Mapping(target = "insuranceDeductibleRate", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "deliveryLocked", ignore = true)
    @Mapping(target = "deliveryCodeHash", ignore = true)
    @Mapping(target = "deliveryCodeSalt", ignore = true)
    @Mapping(target = "deliveryCodeCreatedAt", ignore = true)
    @Mapping(target = "deliveryCodeVerifiedAt", ignore = true)
    @Mapping(target = "deliveryCodeAttempts", ignore = true)
    @Mapping(target = "deliveryCodeEnc", ignore = true)
    @Mapping(target = "deliveryCodeIv", ignore = true)

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)

    Shipment toEntity(CreateShipmentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "status", ignore = true)

    @Mapping(target = "basePrice", ignore = true)
    @Mapping(target = "insuranceFee", ignore = true)
    @Mapping(target = "insuranceCoverageAmount", ignore = true)
    @Mapping(target = "insuranceDeductibleRate", ignore = true)

    @Mapping(target = "pickupLatitude", ignore = true)
    @Mapping(target = "pickupLongitude", ignore = true)
    @Mapping(target = "deliveryLatitude", ignore = true)
    @Mapping(target = "deliveryLongitude", ignore = true)

    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "deliveryLocked", ignore = true)

    @Mapping(target = "deliveryCodeHash", ignore = true)
    @Mapping(target = "deliveryCodeSalt", ignore = true)
    @Mapping(target = "deliveryCodeCreatedAt", ignore = true)
    @Mapping(target = "deliveryCodeVerifiedAt", ignore = true)
    @Mapping(target = "deliveryCodeAttempts", ignore = true)
    @Mapping(target = "deliveryCodeEnc", ignore = true)
    @Mapping(target = "deliveryCodeIv", ignore = true)

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)

    void updateEntity(@MappingTarget Shipment shipment, UpdateShipmentRequest request);

    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(target = "driver", ignore = true)
    ShipmentResponse toResponse(Shipment shipment);
}