package com.shipmate.controller.booking;

import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.service.booking.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;

    // ===================== CREATE BOOKING =====================

    @Operation(
        summary = "Create a booking",
        description = "Allows an authenticated driver to create a booking by selecting one or more available shipments."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or shipment not available"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal(expression = "username") String driverId,
            @Valid @RequestBody CreateBookingRequest request) {

        BookingResponse response =
                bookingService.createBooking(UUID.fromString(driverId), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
