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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final ShipmentMapper shipmentMapper;

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
    
    @Transactional(readOnly = true)
    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentById(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
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
}
