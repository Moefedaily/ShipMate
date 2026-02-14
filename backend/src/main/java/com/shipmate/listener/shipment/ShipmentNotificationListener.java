package com.shipmate.listener.shipment;

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.repository.shipment.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentNotificationListener {

    private final ShipmentRepository shipmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentStatusChanged(ShipmentStatusChangedEvent event) {

        Shipment shipment = shipmentRepository
                .findById(event.shipmentId())
                .orElseThrow();

        if (event.status() != ShipmentStatus.IN_TRANSIT &&
            event.status() != ShipmentStatus.DELIVERED &&
            event.status() != ShipmentStatus.CANCELLED) {
            return;
        }

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        shipment.getSender().getId(),
                        "Shipment update",
                        buildMessage(event.status()),
                        NotificationType.DELIVERY_STATUS
                )
        );

        log.info("[NOTIF] Shipment notification sent shipmentId={} status={}",
                shipment.getId(), event.status());
    }

    private String buildMessage(ShipmentStatus status) {
        return switch (status) {
            case IN_TRANSIT -> "Your shipment is now in transit";
            case DELIVERED -> "Your shipment has been delivered";
            case CANCELLED -> "Your shipment was cancelled";
            default -> "Shipment updated";
        };
    }
}
