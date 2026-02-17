package com.shipmate.mapper.earning;

import com.shipmate.dto.response.earning.DriverEarningResponse;
import com.shipmate.model.earning.DriverEarning;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DriverEarningMapper {
    
    @Mapping(target = "id", source = "id")
    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "shipmentId", source = "shipment.id")
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "grossAmount", source = "grossAmount")
    @Mapping(target = "commissionAmount", source = "commissionAmount")
    @Mapping(target = "netAmount", source = "netAmount")
    @Mapping(target = "payoutStatus", source = "payoutStatus")
    @Mapping(target = "createdAt", source = "createdAt")
    DriverEarningResponse toResponse(DriverEarning earning);
}