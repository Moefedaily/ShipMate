package com.shipmate.model.vehicle;

import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.user.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false)
    private DriverProfile driverProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "max_weight_capacity", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxWeightCapacity;

    @Column(name = "plate_number", length = 20)
    private String plateNumber;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "vehicle_description", columnDefinition = "text")
    private String vehicleDescription;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.PENDING;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
