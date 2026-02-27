package com.shipmate.service.shipment;

import com.shipmate.dto.request.pricing.PricingRequest;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.listener.booking.BookingStatusChangedEvent;
import com.shipmate.listener.delivery.DeliveryCodeEventPublisher;
import com.shipmate.listener.shipment.ShipmentStatusChangedEvent;
import com.shipmate.mapper.shipment.ShipmentAssembler;
import com.shipmate.mapper.shipment.ShipmentMapper;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.delivery.DeliveryCodeService;
import com.shipmate.service.payment.PaymentService;
import com.shipmate.service.pricing.PricingService;
import com.shipmate.util.GeoUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentAssembler shipmentAssembler;
    private final Cloudinary cloudinary;
    private final PricingService pricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final DeliveryCodeService deliveryCodeService;
    private final DeliveryCodeEventPublisher deliveryCodeEventPublisher;



    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;
    @Value("${app.insurance.deductible-rate}")
    private BigDecimal insuranceDeductibleRate;
    @Value("${app.insurance.tier1.limit}")
    private BigDecimal tier1Limit;

    @Value("${app.insurance.tier1.rate}")
    private BigDecimal tier1Rate;

    @Value("${app.insurance.tier2.limit}")
    private BigDecimal tier2Limit;

    @Value("${app.insurance.tier2.rate}")
    private BigDecimal tier2Rate;
    @Value("${app.insurance.max-declared-value}")
    private BigDecimal maxDeclaredValue;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );


        public ShipmentResponse create(UUID senderId, CreateShipmentRequest request) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentMapper.toEntity(request);
        shipment.setSender(sender);
        shipment.setStatus(ShipmentStatus.CREATED);

        BigDecimal distanceKm = GeoUtils.haversineKm(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDeliveryLatitude(),
                request.getDeliveryLongitude()
        );

        BigDecimal basePrice = pricingService.computeBasePrice(
                new PricingRequest(distanceKm, request.getPackageWeight())
        );

        shipment.setBasePrice(basePrice);


        if (request.isInsuranceSelected()) {

            BigDecimal declaredValue = request.getDeclaredValue();

            if (declaredValue == null ||
                    declaredValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Declared value required");
            }

            declaredValue = declaredValue.setScale(2, RoundingMode.HALF_UP);

            if (declaredValue.compareTo(request.getPackageValue()) > 0) {
                throw new IllegalArgumentException("Declared value cannot exceed package value");
            }

            if (declaredValue.compareTo(maxDeclaredValue) > 0) {
                throw new IllegalArgumentException("Declared value exceeds maximum allowed insurance limit");
            }

            // Tier-based rate
            BigDecimal premiumRate = calculateInsuranceRate(declaredValue);

            BigDecimal insuranceFee = declaredValue
                    .multiply(premiumRate)
                    .setScale(2, RoundingMode.HALF_UP);

            shipment.setInsuranceSelected(true);
            shipment.setDeclaredValue(declaredValue);
            shipment.setInsuranceFee(insuranceFee);
            shipment.setInsuranceCoverageAmount(declaredValue);
            shipment.setInsuranceDeductibleRate(insuranceDeductibleRate);

        } else {

            shipment.setInsuranceSelected(false);
            shipment.setDeclaredValue(null);
            shipment.setInsuranceFee(BigDecimal.ZERO);
            shipment.setInsuranceCoverageAmount(null);
            shipment.setInsuranceDeductibleRate(null);
        }

        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        return shipmentAssembler.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getMyShipments(UUID senderId, Pageable pageable) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return shipmentRepository
                .findBySender(sender, pageable)
                .map(shipmentAssembler::toResponse);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getMyShipment(UUID shipmentId, UUID senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentRepository.findByIdAndSender(shipmentId, sender)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        return shipmentAssembler.toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentById(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
    }


    public ShipmentResponse update(UUID shipmentId, UUID senderId, UpdateShipmentRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentRepository.findByIdAndSender(shipmentId, sender)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (shipment.getStatus() != ShipmentStatus.CREATED) {
                throw new IllegalStateException("Shipment can no longer be modified");
        }

        shipmentMapper.updateEntity(shipment, request);

        BigDecimal distanceKm = GeoUtils.haversineKm(
                shipment.getPickupLatitude(),
                shipment.getPickupLongitude(),
                shipment.getDeliveryLatitude(),
                shipment.getDeliveryLongitude()
        );

        BigDecimal basePrice = pricingService.computeBasePrice(
                new PricingRequest(distanceKm, shipment.getPackageWeight())
        );

        shipment.setBasePrice(basePrice);

        return shipmentAssembler.toResponse(shipment);
        }


    public void delete(UUID shipmentId, UUID senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentRepository.findByIdAndSender(shipmentId, sender)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new IllegalStateException("Shipment can no longer be deleted");
        }

        shipmentRepository.delete(shipment);
    }
    public ShipmentResponse addPhotos( UUID shipmentId, UUID senderId, List<MultipartFile> files) {
    
        if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("At least one photo is required");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentRepository.findByIdAndSender(shipmentId, sender)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (shipment.getStatus() != ShipmentStatus.CREATED &&
                shipment.getStatus() != ShipmentStatus.ASSIGNED) {
                throw new IllegalStateException("Photos cannot be added in current shipment state");
        }

        if (shipment.getPhotos() == null) {
                shipment.setPhotos(new ArrayList<>());
        }

        for (MultipartFile file : files) {
                validateImage(file);

                try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                        "folder", "shipmate/shipments/" + shipmentId,
                        "resource_type", "image"
                        )
                );

                String secureUrl = (String) uploadResult.get("secure_url");
                shipment.getPhotos().add(secureUrl);

                } catch (IOException e) {
                throw new RuntimeException("Failed to upload shipment photo", e);
                }
        }

        Shipment saved = shipmentRepository.save(shipment);
        return shipmentAssembler.toResponse(saved);
        }

    private void validateImage(MultipartFile file) {
       
        if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > maxFileSize) {
                throw new IllegalArgumentException("File size exceeds maximum limit");
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
                throw new IllegalArgumentException("Invalid image type");
        }
        }

    public ShipmentResponse markInTransit(UUID shipmentId, UUID driverId) {

        Shipment shipment = shipmentRepository
                .findWithBookingAndSender(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        validateDriverAccess(shipment, driverId);

        if (shipment.getStatus() == ShipmentStatus.LOST) {
                throw new IllegalStateException("Lost shipment cannot be modified");
        }

        if (shipment.getStatus() != ShipmentStatus.ASSIGNED) {
                throw new IllegalStateException("Shipment cannot move to IN_TRANSIT");
        }

        if (shipment.getBooking().getStatus() != BookingStatus.IN_PROGRESS) {
                throw new IllegalStateException("Booking must be IN_PROGRESS");
        }

        Payment payment = paymentRepository.findByShipment(shipment)
                .orElseThrow(() -> new IllegalStateException("Payment not initialized"));

        if (payment.getPaymentStatus() != PaymentStatus.AUTHORIZED &&
                payment.getPaymentStatus() != PaymentStatus.CAPTURED) {

                throw new IllegalStateException("Shipment payment is not authorized");
        }

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);

        eventPublisher.publishEvent(
                new ShipmentStatusChangedEvent(
                        shipment.getId(),
                        ShipmentStatus.IN_TRANSIT
                )
        );

        return shipmentAssembler.toResponse(shipment);
        }

    public ShipmentResponse markDelivered(UUID shipmentId, UUID driverId) {

        Shipment shipment = shipmentRepository
                .findWithBookingAndSender(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        validateDriverAccess(shipment, driverId);

        if (shipment.getStatus() == ShipmentStatus.LOST) {
                throw new IllegalStateException("Lost shipment cannot be modified");
        }

        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
                throw new IllegalStateException("Shipment must be IN_TRANSIT to deliver");
        }

        shipment.setStatus(ShipmentStatus.DELIVERED);
        if (shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(Instant.now());
        }

        eventPublisher.publishEvent(
                new ShipmentStatusChangedEvent(
                        shipment.getId(),
                        ShipmentStatus.DELIVERED
                )
        );

        paymentService.capturePaymentForShipment(shipment);

        recalculateBookingStatus(shipment, driverId);

        return shipmentAssembler.toResponse(shipment);
        }

   public ShipmentResponse confirmDelivery( UUID shipmentId, UUID driverId, String plainCode) {

        Shipment shipment = shipmentRepository
                .findWithBookingAndSender(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        validateDriverAccess(shipment, driverId);

        if (shipment.getStatus() == ShipmentStatus.LOST) {
            throw new IllegalStateException("Lost shipment cannot be modified");
        }
        

        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalStateException("Shipment must be IN_TRANSIT to confirm delivery");
        }

        Payment payment = paymentRepository.findByShipment(shipment)
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        if (payment.getPaymentStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment not authorized");
        }

        deliveryCodeService.verify(shipment, plainCode);

        paymentService.capturePaymentForShipment(shipment);

        shipment.setStatus(ShipmentStatus.DELIVERED);
        if (shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(Instant.now());
        }
        eventPublisher.publishEvent(
                new ShipmentStatusChangedEvent(
                        shipment.getId(),
                        ShipmentStatus.DELIVERED
                )
        );
        recalculateBookingStatus(shipment, driverId);

        log.info(
                "[DELIVERY] Confirmed shipmentId={} by driverId={}",
                shipmentId,
                driverId
        );

        return shipmentAssembler.toResponse(shipment);
    }

    public void resetDeliveryCode(UUID shipmentId, UUID senderId) {

        Shipment shipment = shipmentRepository
                .findWithBookingAndSender(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (!shipment.getSender().getId().equals(senderId)) {
            throw new AccessDeniedException("Not authorized");
        }

        String newCode = deliveryCodeService.reset(shipmentId, senderId);

        if (newCode != null) {
            deliveryCodeEventPublisher.publishToSender(
                    shipment.getSender().getId(),
                    shipment.getId(),
                    newCode
            );
        }
    }

   public ShipmentResponse cancelShipment(UUID shipmentId, UUID actorId) {

        Shipment shipment = shipmentRepository
                .findWithBookingAndSender(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        validateParticipantAccess(shipment, actorId);

        if (shipment.getStatus() == ShipmentStatus.LOST) {
                throw new IllegalStateException("Lost shipment cannot be modified");
        }

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
                throw new IllegalStateException("Delivered shipment cannot be cancelled");
        }

        Optional<Payment> paymentOpt = paymentRepository.findByShipment(shipment);

        if (paymentOpt.isPresent() &&
        paymentOpt.get().getPaymentStatus() == PaymentStatus.CAPTURED) {

        throw new IllegalStateException("Captured shipment cannot be cancelled");
        }

        paymentService.handleCancellation(shipment);

        shipment.setStatus(ShipmentStatus.CANCELLED);

        eventPublisher.publishEvent(
                new ShipmentStatusChangedEvent(
                        shipment.getId(),
                        ShipmentStatus.CANCELLED
                )
        );

        recalculateBookingStatus(shipment, actorId);

        return shipmentAssembler.toResponse(shipment);
        }
    private void validateDriverAccess(Shipment shipment, UUID driverId) {

        if (shipment.getBooking().getDriver() == null ||
            !shipment.getBooking().getDriver().getId().equals(driverId)) {

            throw new AccessDeniedException("Only assigned driver can update shipment");
        }
    }

    private void validateParticipantAccess(Shipment shipment, UUID userId) {

        boolean isDriver =
                shipment.getBooking().getDriver() != null &&
                shipment.getBooking().getDriver().getId().equals(userId);

        boolean isSender =
                shipment.getSender().getId().equals(userId);

        if (!isDriver && !isSender) {
            throw new AccessDeniedException("Not authorized");
        }
    }

        private void recalculateBookingStatus(Shipment shipment, UUID actorId) {

        var booking = shipment.getBooking();

        var shipments = booking.getShipments();

        boolean allCancelled = shipments.stream()
                .allMatch(s -> s.getStatus() == ShipmentStatus.CANCELLED);

        boolean allDeliveredOrLost = shipments.stream()
        .allMatch(s ->
                s.getStatus() == ShipmentStatus.DELIVERED ||
                s.getStatus() == ShipmentStatus.LOST
        );
        boolean allFinished = shipments.stream()
                .allMatch(s ->
                        s.getStatus() == ShipmentStatus.DELIVERED ||
                        s.getStatus() == ShipmentStatus.CANCELLED ||
                        s.getStatus() == ShipmentStatus.LOST
                );

        BookingStatus previousStatus = booking.getStatus();
        BookingStatus newStatus = previousStatus;

        if (allCancelled) {
                newStatus = BookingStatus.CANCELLED;
        }
        else if (allDeliveredOrLost || allFinished) {
                newStatus = BookingStatus.COMPLETED;
        }
        else {
                if (previousStatus == BookingStatus.CONFIRMED ||
                previousStatus == BookingStatus.IN_PROGRESS) {

                newStatus = BookingStatus.IN_PROGRESS;
                }
        }

        if (newStatus != previousStatus) {

                booking.setStatus(newStatus);
                bookingRepository.save(booking);

                eventPublisher.publishEvent(
                        new BookingStatusChangedEvent(
                                booking.getId(),
                                newStatus,
                                actorId
                        )
                );

                log.info(
                        "[BOOKING] recalculated status bookingId={} from {} to {}",
                        booking.getId(),
                        previousStatus,
                        newStatus
                );
        }
        }

        private BigDecimal calculateInsuranceRate(BigDecimal declaredValue) {

        if (declaredValue.compareTo(tier1Limit) <= 0) {
            return tier1Rate;
        }

        if (declaredValue.compareTo(tier2Limit) <= 0) {
            return tier2Rate;
        }

        throw new IllegalArgumentException("Declared value exceeds supported insurance tiers");
    }
}
