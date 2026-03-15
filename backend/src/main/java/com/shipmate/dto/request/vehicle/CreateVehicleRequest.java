package com.shipmate.dto.request.vehicle;

import com.shipmate.model.user.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleRequest {

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Max weight capacity is required")
    @Positive(message = "Max weight capacity must be positive")
    private BigDecimal maxWeightCapacity;

    private String plateNumber;

    private LocalDate insuranceExpiry;

    private String vehicleDescription;
}
