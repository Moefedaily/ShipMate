package com.shipmate.model.shipment;

import com.shipmate.model.booking.Booking;
import com.shipmate.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal pickupLongitude;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal deliveryLongitude;

    @Column(name = "package_description")
    private String packageDescription;

    @Column(name = "package_weight", nullable = false, precision = 6, scale = 2)
    private BigDecimal packageWeight;

    @Column(name = "package_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal packageValue;

    @Column(name = "requested_pickup_date", nullable = false)
    private LocalDate requestedPickupDate;

    @Column(name = "requested_delivery_date", nullable = false)
    private LocalDate requestedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "base_price", nullable = false, precision = 8, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "pickup_order")
    private Integer pickupOrder;

    @Column(name = "delivery_order")
    private Integer deliveryOrder;

    @Column(name = "extra_insurance_fee", precision = 8, scale = 2)
    private BigDecimal extraInsuranceFee;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> photos;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
