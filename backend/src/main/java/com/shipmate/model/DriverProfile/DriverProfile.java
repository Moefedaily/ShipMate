package com.shipmate.model.DriverProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.shipmate.model.user.User;
import com.shipmate.model.user.VehicleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal maxWeightCapacity;

    @Column(columnDefinition = "text")
    private String vehicleDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.PENDING;

    @Column(name = "strike_count", nullable = false)
    @Builder.Default
    private int strikeCount = 0;

    private Instant approvedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Column(name = "last_latitude", precision = 9, scale = 6)
    private BigDecimal lastLatitude;

    @Column(name = "last_longitude", precision = 9, scale = 6)
    private BigDecimal lastLongitude;

    @Column(name = "last_location_updated_at")
    private Instant lastLocationUpdatedAt;

}
