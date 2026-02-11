package com.shipmate.controller.message;

import com.shipmate.dto.request.message.SendMessageRequest;
import com.shipmate.dto.response.message.MessageResponse;
import com.shipmate.service.message.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings/{bookingId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;


    @Operation(
        summary = "Get booking messages",
        description = "Retrieve chat messages for a booking. Accessible only to the booking driver or senders owning shipments in the booking."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping
    public ResponseEntity<Page<MessageResponse>> getBookingMessages(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal(expression = "username") String userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                messageService.getBookingMessages(
                        bookingId,
                        UUID.fromString(userId),
                        pageable
                )
        );
    }

    @Operation(summary = "Mark messages as read")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Messages marked as read"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        messageService.markMessagesAsRead(
                bookingId,
                UUID.fromString(userId)
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Send a message to a booking",
        description = "Send a message to a booking. Accessible only to the booking driver or senders owning shipments in the booking."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.ok(
                messageService.sendMessage(
                        bookingId,
                        UUID.fromString(userId),
                        request
                )
        );
    }

}
