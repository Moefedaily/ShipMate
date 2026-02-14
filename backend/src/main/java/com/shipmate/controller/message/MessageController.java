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
@RequestMapping("/api/shipments/{shipmentId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;


    @Operation(
        summary = "Get shipment messages",
        description = "Retrieve chat messages for a shipment. Accessible only to the shipment sender or assigned driver."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @GetMapping
    public ResponseEntity<Page<MessageResponse>> getShipmentMessages(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                messageService.getShipmentMessages(
                        shipmentId,
                        UUID.fromString(userId),
                        pageable
                )
        );
    }


    @Operation(summary = "Mark shipment messages as read")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Messages marked as read"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        messageService.markMessagesAsRead(
                shipmentId,
                UUID.fromString(userId)
        );

        return ResponseEntity.noContent().build();
    }


    @Operation(
        summary = "Send a message to a shipment",
        description = "Send a chat message for a shipment. Accessible only to the shipment sender or assigned driver."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.ok(
                messageService.sendMessage(
                        shipmentId,
                        UUID.fromString(userId),
                        request
                )
        );
    }
}
