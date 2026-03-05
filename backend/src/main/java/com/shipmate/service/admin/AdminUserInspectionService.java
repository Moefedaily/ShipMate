package com.shipmate.service.admin;

import com.shipmate.dto.response.insurance.InsuranceClaimResponse;
import com.shipmate.dto.response.payment.PaymentResponse;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.mapper.insurance.InsuranceClaimMapper;
import com.shipmate.mapper.payment.PaymentMapper;
import com.shipmate.mapper.shipment.ShipmentAssembler;

import com.shipmate.model.user.User;

import com.shipmate.repository.user.UserRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.insurance.InsuranceClaimRepository;


import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserInspectionService {

    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final InsuranceClaimRepository claimRepository;

    private final ShipmentAssembler shipmentAssembler;
    private final PaymentMapper paymentMapper;
    private final InsuranceClaimMapper claimMapper;

    public Page<ShipmentResponse> getUserShipments(
            UUID userId,
            Pageable pageable
    ) {

        userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return shipmentRepository
                .findAllUserShipments(userId, pageable)
                .map(shipmentAssembler::toResponse);
    }

    public Page<InsuranceClaimResponse> getUserClaims(
            UUID userId,
            Pageable pageable
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return claimRepository
                .findByClaimant(user, pageable)
                .map(claimMapper::toResponse);
    }

    public Page<PaymentResponse> getUserPayments(
            UUID userId,
            Pageable pageable
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return paymentRepository
                .findBySender(user, pageable)
                .map(paymentMapper::toResponse);
    }
}