package com.shipmate.service.shipment;

import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.mapper.ShipmentMapper;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final ShipmentMapper shipmentMapper;
    private final Cloudinary cloudinary;
    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );


    /* =========================
       CREATE
       ========================= */

    public ShipmentResponse create(UUID senderId, CreateShipmentRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.debug("request: {}", request);
        Shipment shipment = shipmentMapper.toEntity(request);
        shipment.setSender(sender);
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setExtraInsuranceFee(BigDecimal.ZERO);

        log.debug("shipment: {}", shipment);
        Shipment saved = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(saved);
    }

    /* =========================
       READ
       ========================= */

    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getMyShipments(UUID senderId, Pageable pageable) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return shipmentRepository
                .findBySender(sender, pageable)
                .map(shipmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getMyShipment(UUID shipmentId, UUID senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentRepository.findByIdAndSender(shipmentId, sender)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        return shipmentMapper.toResponse(shipment);
    }

    /* =========================
       UPDATE
       ========================= */

    public ShipmentResponse update(UUID shipmentId, UUID senderId, UpdateShipmentRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Shipment shipment = shipmentRepository.findByIdAndSender(shipmentId, sender)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new IllegalStateException("Shipment can no longer be modified");
        }

        shipmentMapper.updateEntity(shipment, request);

        return shipmentMapper.toResponse(shipment);
    }

    /* =========================
       DELETE
       ========================= */

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

        // Status guard
        if (shipment.getStatus() != ShipmentStatus.CREATED &&
                shipment.getStatus() != ShipmentStatus.ASSIGNED) {
                throw new IllegalStateException("Photos cannot be added in current shipment state");
        }

        // Init photos list if null
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
        return shipmentMapper.toResponse(saved);
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

}
