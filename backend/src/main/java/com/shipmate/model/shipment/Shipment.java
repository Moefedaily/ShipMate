package com.shipmate.model.shipment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.shipmate.model.booking.Booking;
import com.shipmate.model.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Column(name = "pickup_address", columnDefinition = "TEXT", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_latitude", precision = 9, scale = 6)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 9, scale = 6)
    private BigDecimal pickupLongitude;

    @Column(name = "delivery_address", columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_latitude", precision = 9, scale = 6)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 9, scale = 6)
    private BigDecimal deliveryLongitude;

    @Column(name = "package_description", columnDefinition = "TEXT")
    private String packageDescription;

    @Column(name = "package_weight", precision = 6, scale = 2)
    private BigDecimal packageWeight;

    @Column(name = "package_value", precision = 10, scale = 2)
    private BigDecimal packageValue;

    @Column(name = "requested_pickup_date")
    private LocalDate requestedPickupDate;

    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "pickup_order")
    private Integer pickupOrder;

    @Column(name = "delivery_order")
    private Integer deliveryOrder;

    @Column(name = "base_price", precision = 8, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "extra_insurance_fee", precision = 8, scale = 2)
    private BigDecimal extraInsuranceFee;

    @Column(columnDefinition = "TEXT") // We'll store the JSONB as String for now
    private String photos;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
