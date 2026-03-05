package com.shipmate.controller.payment;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.dto.response.admin.AdminPaymentResponse;
import com.shipmate.dto.response.payment.AdminRefundResponse;
import com.shipmate.service.admin.AdminPaymentService;
import com.shipmate.service.payment.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final AdminPaymentService adminPaymentService;

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund payment", description = "Initiate a refund for a specific payment")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Refund request accepted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })  
    public ResponseEntity<AdminRefundResponse> refundPayment(
            @PathVariable UUID paymentId
    ) {

        paymentService.refundByAdmin(paymentId);

        return ResponseEntity.accepted()
                .body(new AdminRefundResponse(paymentId, "REFUND_REQUESTED"));
    }

    @GetMapping
    @Operation(summary = "List payments", description = "List all payments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payments listed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    
    public ResponseEntity<Page<AdminPaymentResponse>> getPayments(
            Pageable pageable
    ) {

        Page<AdminPaymentResponse> response =
                adminPaymentService.getPayments(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details", description = "Retrieve details of a specific payment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<AdminPaymentResponse> getPayment(
            @PathVariable UUID paymentId
    ) {

        AdminPaymentResponse response =
                adminPaymentService.getPayment(paymentId);

        return ResponseEntity.ok(response);
    }
}

