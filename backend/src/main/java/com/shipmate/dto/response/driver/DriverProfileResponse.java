package com.shipmate.dto.response.driver;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.user.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileResponse {

    private UUID  id;
    private String licenseNumber;
    private VehicleType vehicleType;
    private BigDecimal maxWeightCapacity;
    private String vehicleDescription;
    private DriverStatus status;
    private BigDecimal lastLatitude;
    private BigDecimal lastLongitude;
    private Instant lastLocationUpdatedAt;
    private Instant createdAt;
    private Instant approvedAt;
}
