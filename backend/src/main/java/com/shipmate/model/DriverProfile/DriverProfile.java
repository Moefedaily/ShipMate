package com.shipmate.model.DriverProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.shipmate.model.vehicle.Vehicle;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.shipmate.model.photo.Photo;
import com.shipmate.model.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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

    @Column(unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "pending_license_number", length = 50)
    private String pendingLicenseNumber;

    @Builder.Default
    @OneToMany(mappedBy = "driverProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Photo> licensePhotos = new ArrayList<>();

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "pending_license_expiry")
    private LocalDate pendingLicenseExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.PENDING;

    @Builder.Default
    @OneToMany(mappedBy = "driverProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehicle> vehicles = new ArrayList<>();

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

    public Vehicle getActiveVehicle() {
        if (vehicles == null) return null;
        return vehicles.stream()
                .filter(Vehicle::isActive)
                .findFirst()
                .orElse(null);
    }

    public boolean hasApprovedLicense() {
        return this.status == DriverStatus.APPROVED;
    }

    public boolean isReadyForBooking() {
        return this.status == DriverStatus.APPROVED && getActiveVehicle() != null;
    }
}
