package com.shipmate.service.insurance;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.shipmate.dto.request.insurance.AdminClaimDecisionRequest;
import com.shipmate.dto.request.insurance.CreateInsuranceClaimRequest;
import com.shipmate.dto.response.insurance.InsuranceClaimResponse;
import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.listener.shipment.ShipmentStatusChangedEvent;
import com.shipmate.mapper.insurance.InsuranceClaimMapper;
import com.shipmate.model.insuranceClaim.*;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.insurance.InsuranceClaimRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.payment.PaymentService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceClaimService {

    @Value("${app.insurance.claim-window-days:7}")
    private long claimWindowDays;

    private final InsuranceClaimRepository claimRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final InsuranceClaimMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final  Cloudinary cloudinary;
    

    public InsuranceClaimResponse submitClaim( UUID shipmentId, UUID claimantId, CreateInsuranceClaimRequest request) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

            System.err.println("shipmentId: " + shipmentId);
        User claimant = userRepository.findById(claimantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        System.err.println("claimantId: " + claimantId);
        if (!shipment.getSender().getId().equals(claimantId)) {
            throw new IllegalStateException("Not your shipment");
        }

        if (claimRepository.existsByShipment(shipment)) {
            throw new IllegalStateException("Claim already exists for this shipment");
        }

        if (!shipment.isInsuranceSelected()
                || shipment.getInsuranceFee() == null
                || shipment.getInsuranceFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Shipment not insured");
        }

        if (request.getClaimReason() == null) {
            throw new IllegalArgumentException("Claim reason is required");
        }

        switch (request.getClaimReason()) {
            case LOST -> {
                if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT
                        && shipment.getStatus() != ShipmentStatus.LOST) {
                    throw new IllegalStateException("LOST claim allowed only for IN_TRANSIT or LOST shipments");
                }
            }
            case DAMAGED -> {
                if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
                    throw new IllegalStateException("DAMAGED claim allowed only for DELIVERED shipments");
                }
            }
            case OTHER -> {
                if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
                    throw new IllegalStateException("OTHER claims allowed only for DELIVERED shipments");
                }
            }
        }

        Instant anchor;
        System.out.println("STEP 4 - Claim reason: " + request.getClaimReason());

        if (request.getClaimReason() == ClaimReason.DAMAGED
                || request.getClaimReason() == ClaimReason.OTHER) {

            anchor = shipment.getDeliveredAt();
        System.out.println("STEP 5 - Anchor computed: " + anchor);

            if (anchor == null) {
                throw new IllegalStateException("Shipment not delivered yet");
            }

        } else { // LOST

            anchor = shipment.getUpdatedAt();

            if (anchor == null) {
                throw new IllegalStateException("Shipment timestamp missing");
            }
        }
        Instant deadline = anchor.plusSeconds(claimWindowDays * 24 * 3600);
        if (Instant.now().isAfter(deadline)) {
            throw new IllegalStateException("Claim window expired");
        }
        System.out.println("STEP 6 - Deadline: " + deadline);


        BigDecimal declaredValueSnapshot = shipment.getDeclaredValue();
        BigDecimal coverageAmount = shipment.getInsuranceCoverageAmount();
        BigDecimal deductibleRateSnapshot = shipment.getInsuranceDeductibleRate();

        System.out.println("STEP 7 - Declared value: " + declaredValueSnapshot);
        System.out.println("STEP 8 - Coverage amount: " + coverageAmount);
        System.out.println("STEP 9 - Deductible rate: " + deductibleRateSnapshot);

        if (declaredValueSnapshot == null || declaredValueSnapshot.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Shipment declared value missing");
        }
        if (coverageAmount == null || coverageAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Shipment insurance coverage missing");
        }
        if (deductibleRateSnapshot == null || deductibleRateSnapshot.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Shipment deductible rate missing");
        }

        BigDecimal deductibleAmount = coverageAmount
                .multiply(deductibleRateSnapshot)
                .setScale(2, RoundingMode.HALF_UP);

        System.err.println("STEP 10 - Deductible amount: " + deductibleAmount);
        BigDecimal compensation = coverageAmount
                .subtract(deductibleAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        System.err.println("STEP 11 - Compensation amount: " + compensation);
        InsuranceClaim claim = InsuranceClaim.builder()
                .shipment(shipment)
                .claimant(claimant)
                .declaredValueSnapshot(declaredValueSnapshot)
                .coverageAmount(coverageAmount)
                .deductibleRate(deductibleRateSnapshot)
                .compensationAmount(compensation)
                .claimReason(request.getClaimReason())
                .claimDescription(request.getDescription())
                .photos(new ArrayList<>())
                .claimStatus(ClaimStatus.SUBMITTED)
                .build();

        claimRepository.save(claim);
    System.out.println("STEP 10 - Claim saved successfully");
        return mapper.toResponse(claim);
    }

    @Transactional
    public InsuranceClaimResponse reviewClaim(
            UUID claimId,
            UUID adminUserId,
            AdminClaimDecisionRequest request
    ) {

        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));

        if (claim.getClaimStatus() != ClaimStatus.SUBMITTED &&
            claim.getClaimStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Claim already processed");
        }

        if (request.getDecision() != ClaimStatus.APPROVED &&
            request.getDecision() != ClaimStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid decision");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        claim.setAdminUser(admin);
        claim.setAdminNotes(request.getAdminNotes());
        claim.setResolvedAt(Instant.now());

        if (request.getDecision() == ClaimStatus.APPROVED) {

            claim.setClaimStatus(ClaimStatus.APPROVED);
            eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                    claim.getClaimant().getId(),
                    "Insurance claim approved",
                    "Your insurance claim for shipment " + claim.getShipment().getId() + " has been approved. Refund is being processed.",
                    NotificationType.INSURANCE_UPDATE,
                    claim.getShipment().getId(),
                    ReferenceType.INSURANCE
                )
            );

            if (claim.getClaimReason() == ClaimReason.LOST) {

                Shipment shipment = claim.getShipment();

                shipment.setStatus(ShipmentStatus.LOST);
                shipmentRepository.save(shipment);
                eventPublisher.publishEvent(
                    new ShipmentStatusChangedEvent(
                        shipment.getId(),
                        ShipmentStatus.LOST
                    )
                );
            }

            Payment payment = paymentRepository
                    .findByShipment(claim.getShipment())
                    .orElseThrow(() -> new IllegalStateException("Payment not found"));

            paymentService.refundByAdmin(payment.getId());

        } else {

            claim.setClaimStatus(ClaimStatus.REJECTED);
            eventPublisher.publishEvent(
            new NotificationRequestedEvent(
                claim.getClaimant().getId(),
                "Insurance claim rejected",
                "Your insurance claim has been rejected. Please check admin notes for details.",
                NotificationType.INSURANCE_UPDATE,
                claim.getShipment().getId(),
                ReferenceType.INSURANCE
            )
        );
        }

        return mapper.toResponse(claim);
    }

    @Transactional(readOnly = true)
    public InsuranceClaimResponse getByShipment(UUID shipmentId) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        InsuranceClaim claim = claimRepository.findByShipment(shipment)
                .orElseThrow(() -> new ResponseStatusException( HttpStatus.NOT_FOUND,"No claim found"));

        return mapper.toResponse(claim);
    }

    @Transactional(readOnly = true)
    public List<InsuranceClaimResponse> listClaims(ClaimStatus status) {

        List<InsuranceClaim> claims;

        if (status != null) {
            claims = claimRepository.findByClaimStatus(status);
        } else {
            claims = claimRepository.findAll();
        }

        return claims.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InsuranceClaimResponse getById(UUID id) {

        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));

        return mapper.toResponse(claim);
    }

    public InsuranceClaimResponse addClaimPhotos(UUID claimId, UUID userId, List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one photo is required");
        }

        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));

        if (!claim.getClaimant().getId().equals(userId)) {
            throw new IllegalStateException("Not your claim");
        }

        if (claim.getClaimStatus() != ClaimStatus.SUBMITTED &&
            claim.getClaimStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Cannot upload photos for this claim state");
        }

        if (claim.getPhotos() == null) {
            claim.setPhotos(new ArrayList<>());
        }

        for (MultipartFile file : files) {

            validateImage(file);

            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                        "folder", "shipmate/claims/" + claimId,
                        "resource_type", "image"
                    )
                );

                String secureUrl = (String) uploadResult.get("secure_url");
                claim.getPhotos().add(secureUrl);

            } catch (IOException e) {
                throw new RuntimeException("Failed to upload claim photo", e);
            }
        }

        return mapper.toResponse(claimRepository.save(claim));
    }
    private void validateImage(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        if (file.getSize() > 5_000_000) {
            throw new IllegalArgumentException("File too large (max 5MB)");
        }
    }

    @Transactional(readOnly = true)
    public List<InsuranceClaimResponse> listMyClaims(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return claimRepository.findByClaimant(user)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}