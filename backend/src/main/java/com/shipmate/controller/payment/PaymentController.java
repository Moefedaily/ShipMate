package com.shipmate.controller.payment;

import com.shipmate.dto.response.payment.CreatePaymentIntentResponse;
import com.shipmate.dto.response.payment.PaymentResponse;
import com.shipmate.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/shipments/{shipmentId}/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {
    
    private final PaymentService paymentService;

    @Operation(
        summary = "Create payment intent",
        description = "Create a Stripe payment intent for a shipment. Returns client secret for completing payment on frontend."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment intent created successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized for this shipment"),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "400", description = "Payment intent already exists or invalid shipment state")
    })
    @PostMapping("/intent")
    public ResponseEntity<CreatePaymentIntentResponse> createPaymentIntent(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        CreatePaymentIntentResponse response =
                paymentService.createPaymentIntent(
                        shipmentId,
                        UUID.fromString(userId)
                );
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get payment details",
        description = "Retrieve payment information for a shipment. Accessible only to the shipment sender."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized for this shipment"),
        @ApiResponse(responseCode = "404", description = "Shipment or payment not found")
    })
    @GetMapping
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        PaymentResponse response =
                paymentService.getPaymentForShipment(
                        shipmentId,
                        UUID.fromString(userId)
                );
        return ResponseEntity.ok(response);
    }
}