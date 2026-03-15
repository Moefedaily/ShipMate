package com.shipmate.dto.request.driver;

import java.math.BigDecimal;

import com.shipmate.model.user.VehicleType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverApplyRequest {

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotNull(message = "License expiry date is required")
    private LocalDate licenseExpiry;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Max weight capacity is required")
    @DecimalMin(value = "0.1", message = "Max weight capacity must be at least 0.1")
    private BigDecimal maxWeightCapacity;

    private String plateNumber;

    private String vehicleDescription;
}
