package com.shipmate.exception;

import lombok.Getter;

@Getter
public class DriverLocationException extends RuntimeException {

    private final DriverLocationErrorCode code;

    public DriverLocationException(
            DriverLocationErrorCode code,
            String message
    ) {
        super(message);
        this.code = code;
    }

    public static DriverLocationException required() {
        return new DriverLocationException(
                DriverLocationErrorCode.LOCATION_REQUIRED,
                "Driver location is required to continue"
        );
    }

    public static DriverLocationException outdated() {
        return new DriverLocationException(
                DriverLocationErrorCode.LOCATION_OUTDATED,
                "Driver location is outdated and must be refreshed"
        );
    }

    public static DriverLocationException tooFar(double distanceKm) {
        return new DriverLocationException(
                DriverLocationErrorCode.LOCATION_TOO_FAR,
                String.format(
                        "Driver is too far from pickup location (%.1f km away)",
                        distanceKm
                )
        );
    }
}
