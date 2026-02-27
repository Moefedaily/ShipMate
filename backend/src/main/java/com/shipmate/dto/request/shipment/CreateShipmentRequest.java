package com.shipmate.dto.request.shipment;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateShipmentRequest {

    @NotBlank
    private String pickupAddress;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal pickupLatitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal pickupLongitude;

    @NotBlank
    private String deliveryAddress;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal deliveryLatitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal deliveryLongitude;

    private String packageDescription;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal packageWeight;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal packageValue;

    @NotNull
    private LocalDate requestedPickupDate;

    @NotNull
    private LocalDate requestedDeliveryDate;

    private boolean insuranceSelected;
    
    private BigDecimal declaredValue;

}
