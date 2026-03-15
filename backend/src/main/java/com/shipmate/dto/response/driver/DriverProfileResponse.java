package com.shipmate.dto.response.driver;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.shipmate.dto.response.vehicle.VehicleResponse;
import com.shipmate.model.DriverProfile.DriverStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfileResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String licenseNumber;
    private String licensePhotoUrl;
    private List<String> licensePhotoUrls;
    private LocalDate licenseExpiry;
    private DriverStatus status;
    private int strikeCount;
    private List<VehicleResponse> vehicles;
    private VehicleResponse activeVehicle;
    private Double lastLatitude;
    private Double lastLongitude;
    private Instant lastLocationUpdatedAt;
    private Instant createdAt;
    private Instant approvedAt;
}
