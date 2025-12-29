package com.shipmate.dto.request.shipment;


import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateShipmentRequest {

    @NotBlank
    private String pickupAddress;

    @NotBlank
    private String deliveryAddress;

    private String packageDescription;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal packageWeight;

    @NotNull
    private LocalDate requestedPickupDate;

    @NotNull
    private LocalDate requestedDeliveryDate;
}
