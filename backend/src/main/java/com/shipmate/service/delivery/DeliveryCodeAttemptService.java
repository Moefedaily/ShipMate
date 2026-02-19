package com.shipmate.service.delivery;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shipmate.model.shipment.Shipment;
import com.shipmate.repository.shipment.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryCodeAttemptService {

    private final ShipmentRepository shipmentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementAttempts(UUID shipmentId) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow();

        Integer attempts = shipment.getDeliveryCodeAttempts();
        shipment.setDeliveryCodeAttempts(
                attempts == null ? 1 : attempts + 1
        );

        shipmentRepository.save(shipment);
    }
}
