package com.shipmate.dto.response.booking;

import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.model.booking.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private UUID id;

    private UUID driverId;

    private BookingStatus status;

    private BigDecimal totalPrice;

    private BigDecimal platformCommission;

    private BigDecimal driverEarnings;

    private List<ShipmentResponse> shipments;

    private Instant createdAt;
}
