package com.shipmate.dto.response.vehicle;

import com.shipmate.model.user.VehicleType;
import com.shipmate.model.vehicle.VehicleStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private UUID id;
    private VehicleType vehicleType;
    private BigDecimal maxWeightCapacity;
    private String plateNumber;
    private LocalDate insuranceExpiry;
    private String vehicleDescription;
    private String rejectionReason;
    private VehicleStatus status;
    private boolean active;
    private Instant createdAt;
}
