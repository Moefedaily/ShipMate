package com.shipmate.dto.response.shipment;


import com.shipmate.dto.response.driver.AssignedDriverResponse;
import com.shipmate.model.shipment.ShipmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ShipmentResponse {

    private UUID id;

    private UUID senderId;

    private String pickupAddress;
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;

    private String deliveryAddress;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;

    private String packageDescription;
    private BigDecimal packageWeight;
    private BigDecimal packageValue;

    private LocalDate requestedPickupDate;
    private LocalDate requestedDeliveryDate;

    private Integer pickupOrder;
    private Integer deliveryOrder;

    private List<String> photos;

    private ShipmentStatus status;
    private AssignedDriverResponse driver;

    private BigDecimal basePrice;

    private Instant createdAt;
    private Instant updatedAt;
}
