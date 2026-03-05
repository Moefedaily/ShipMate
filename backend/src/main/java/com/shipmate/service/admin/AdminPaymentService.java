package com.shipmate.service.admin;

import com.shipmate.dto.response.admin.AdminPaymentResponse;
import com.shipmate.mapper.payment.AdminPaymentMapper;
import com.shipmate.model.payment.Payment;
import com.shipmate.repository.payment.PaymentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final AdminPaymentMapper adminPaymentMapper;

    public Page<AdminPaymentResponse> getPayments(Pageable pageable) {

        return paymentRepository
                .findAll(pageable)
                .map(adminPaymentMapper::toResponse);
    }

    public AdminPaymentResponse getPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment not found"));

        return adminPaymentMapper.toResponse(payment);
    }
    
}