package com.shipmate.service.delivery;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shipmate.listener.delivery.DeliveryLockedEvent;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.repository.shipment.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryCodeAttemptService {

    private final ShipmentRepository shipmentRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int incrementAttemptsAndLockIfNeeded(UUID shipmentId, int maxAttempts) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow();

        int attempts = shipment.getDeliveryCodeAttempts() == null
                ? 1
                : shipment.getDeliveryCodeAttempts() + 1;

        shipment.setDeliveryCodeAttempts(attempts);

        if (attempts >= maxAttempts && !shipment.isDeliveryLocked()) {

            shipment.setDeliveryLocked(true);
            shipmentRepository.save(shipment);

            eventPublisher.publishEvent(
                new DeliveryLockedEvent(
                    shipment.getId(),
                    shipment.getBooking().getId(),
                    shipment.getSender().getId(),
                    shipment.getBooking().getDriver().getId()
                )
            );
        } else {
            shipmentRepository.save(shipment);
        }

        return attempts;
    }
}