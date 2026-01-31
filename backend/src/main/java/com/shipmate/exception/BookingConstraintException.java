package com.shipmate.exception;

import lombok.Getter;

@Getter
public class BookingConstraintException extends RuntimeException {

    private final BookingConstraintErrorCode code;

    public BookingConstraintException(
            BookingConstraintErrorCode code,
            String message
    ) {
        super(message);
        this.code = code;
    }

    public static BookingConstraintException locked() {
        return new BookingConstraintException(
                BookingConstraintErrorCode.BOOKING_LOCKED,
                "Booking is already confirmed or in progress"
        );
    }

    public static BookingConstraintException capacityExceeded() {
        return new BookingConstraintException(
                BookingConstraintErrorCode.CAPACITY_EXCEEDED,
                "Vehicle capacity exceeded"
        );
    }

    public static BookingConstraintException shipmentLimitExceeded(int max) {
        return new BookingConstraintException(
                BookingConstraintErrorCode.SHIPMENT_LIMIT_EXCEEDED,
                "Maximum of " + max + " shipments per trip exceeded"
        );
    }

    public static BookingConstraintException tripDistanceExceeded() {
        return new BookingConstraintException(
                BookingConstraintErrorCode.TRIP_DISTANCE_CAP_EXCEEDED,
                "Trip distance exceeds allowed limit"
        );
    }
}
