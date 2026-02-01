package com.shipmate.dev.fixtures;

import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.user.User;
import com.shipmate.model.user.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;

public final class DriverFixtureFactory {

    public static DriverProfile approvedDriver(
            User driver,
            GrenobleLocations.GeoPoint location,
            int index
    ) {

        return DriverProfile.builder()
                .user(driver)
                .licenseNumber("DEV-LIC-" + index)
                .vehicleType(VehicleType.CAR)
                .maxWeightCapacity(BigDecimal.valueOf(50))
                .vehicleDescription("Dev vehicle " + index)

                .status(DriverStatus.APPROVED)
                .approvedAt(Instant.now())

                .lastLatitude(BigDecimal.valueOf(location.lat()))
                .lastLongitude(BigDecimal.valueOf(location.lng()))
                .lastLocationUpdatedAt(Instant.now())

                .build();
    }

    private DriverFixtureFactory() {}
}
