package com.shipmate.controller.payment;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.dto.response.payment.AdminRefundResponse;
import com.shipmate.service.payment.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<AdminRefundResponse> refundPayment(
            @PathVariable UUID paymentId
    ) {

        paymentService.refundByAdmin(paymentId);

        return ResponseEntity.accepted()
                .body(new AdminRefundResponse(paymentId, "REFUND_REQUESTED"));
    }
}

