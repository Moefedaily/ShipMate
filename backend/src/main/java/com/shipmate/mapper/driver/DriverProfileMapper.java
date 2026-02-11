package com.shipmate.mapper.driver;

import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.dto.response.driver.DriverProfileResponse;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DriverProfileMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "licenseNumber", source = "request.licenseNumber")
    @Mapping(target = "vehicleType", source = "request.vehicleType")
    @Mapping(target = "maxWeightCapacity", source = "request.maxWeightCapacity")
    @Mapping(target = "vehicleDescription", source = "request.vehicleDescription")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DriverProfile toEntity(DriverApplyRequest request, User user);
    
    @Mapping(target = "id", source = "id")
    @Mapping(target = "licenseNumber", source = "licenseNumber")
    @Mapping(target = "vehicleType", source = "vehicleType")
    @Mapping(target = "maxWeightCapacity", source = "maxWeightCapacity")
    @Mapping(target = "vehicleDescription", source = "vehicleDescription")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "lastLatitude", source = "lastLatitude")
    @Mapping(target = "lastLongitude", source = "lastLongitude")
    @Mapping(target = "lastLocationUpdatedAt", source = "lastLocationUpdatedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "approvedAt", source = "approvedAt")
    DriverProfileResponse toResponse(DriverProfile entity);
}