package com.shipmate.mapper.vehicle;

import com.shipmate.dto.request.vehicle.CreateVehicleRequest;
import com.shipmate.dto.response.vehicle.VehicleResponse;
import com.shipmate.model.vehicle.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driverProfile", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "active", constant = "false")
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vehicle toEntity(CreateVehicleRequest request);

    VehicleResponse toResponse(Vehicle vehicle);
}
