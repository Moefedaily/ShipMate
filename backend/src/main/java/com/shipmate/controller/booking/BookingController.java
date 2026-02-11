package com.shipmate.controller.booking;

import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.mapper.booking.BookingMapper;
import com.shipmate.model.booking.Booking;
import com.shipmate.service.booking.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
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
    private final BookingMapper bookingMapper;

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

        Booking booking  =bookingService.createBooking(UUID.fromString(driverId), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingMapper.toResponse(booking));
    }

    // ===================== CONFIRM BOOKING =====================
    @Operation(summary = "Confirm a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid booking state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirm(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "username") String driverId) {

         Booking booking = bookingService.confirm(id, UUID.fromString(driverId));       

        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    // ===================== START BOOKING =====================

    @Operation(summary = "Start a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking started"),
        @ApiResponse(responseCode = "400", description = "Invalid booking state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<BookingResponse> start(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "username") String driverId) {

        Booking booking = bookingService.start(id, UUID.fromString(driverId));

        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    // ===================== COMPLETE BOOKING =====================

    @Operation(summary = "Complete a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking completed"),
        @ApiResponse(responseCode = "400", description = "Invalid booking state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "username") String driverId) {

        Booking booking = bookingService.complete(id, UUID.fromString(driverId));

        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    // ===================== CANCEL BOOKING =====================

    @Operation(summary = "Cancel a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled"),
        @ApiResponse(responseCode = "400", description = "Invalid booking state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "username") String driverId) {

        Booking booking = bookingService.cancel(id, UUID.fromString(driverId));

        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    // ===================== GET MY BOOKINGS =====================
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal(expression = "username") String driverId) {

        List<Booking> bookings =
                bookingService.getMyBookings(UUID.fromString(driverId));

        return ResponseEntity.ok(
                bookings.stream()
                        .map(bookingMapper::toResponse)
                        .toList()
        );
    }

    // ===================== GET MY BOOKING =====================
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getMyBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "username") String driverId) {

        return ResponseEntity.ok(
            bookingService.getMyBooking(id, UUID.fromString(driverId))
        );
    }

    @GetMapping("/me/active")
    public ResponseEntity<BookingResponse> getMyActiveBooking(
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        Booking booking = bookingService.getMyActiveBooking(UUID.fromString(userId));
        return ResponseEntity.ok(
            booking != null ? bookingMapper.toResponse(booking) : null
        );
    }

}
