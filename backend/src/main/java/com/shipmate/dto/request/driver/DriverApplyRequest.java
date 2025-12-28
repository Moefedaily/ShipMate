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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverApplyRequest {

    @NotBlank
    private String licenseNumber;

    @NotNull
    private VehicleType vehicleType;

    @NotNull
    @DecimalMin(value = "0.1")
    private BigDecimal maxWeightCapacity;

    private String vehicleDescription;
}
