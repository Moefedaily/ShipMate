package com.shipmate.controller.booking;

import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.mapper.BookingMapper;
import com.shipmate.service.booking.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Booking Management", description = "Admin APIs for managing all bookings")
public class AdminBookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    // ===================== GET ALL BOOKINGS =====================

    @Operation(
        summary = "Get all bookings",
        description = "Retrieves a list of all bookings in the system. Admin only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all bookings"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(
                bookingService.getAllBookings()
                        .stream()
                        .map(bookingMapper::toResponse)
                        .toList()
        );
    }

    // ===================== GET BOOKING BY ID =====================

    @Operation(
        summary = "Get booking by ID",
        description = "Retrieves detailed information about a specific booking by its ID. Admin only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved booking"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(
                bookingMapper.toResponse(
                        bookingService.getBookingById(id)
                )
        );
    }
}