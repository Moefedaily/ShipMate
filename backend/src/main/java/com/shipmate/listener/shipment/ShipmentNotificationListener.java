package com.shipmate.listener.shipment;

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.repository.notification.NotificationRepository;
import com.shipmate.repository.shipment.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentNotificationListener {

    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentStatusChanged(ShipmentStatusChangedEvent event) {

        Shipment shipment = shipmentRepository
                .findById(event.shipmentId())
                .orElseThrow();

        if (event.status() != ShipmentStatus.IN_TRANSIT &&
            event.status() != ShipmentStatus.DELIVERED &&
            event.status() != ShipmentStatus.CANCELLED &&
            event.status() != ShipmentStatus.LOST) {
            return;
        }

        UUID shipmentId = shipment.getId();
        UUID senderId = shipment.getSender().getId();

        String title = buildTitle(event.status());
        String message = buildMessage(event.status(), shipmentId);

        boolean alreadyNotified =
                notificationRepository.existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                        senderId,
                        shipmentId,
                        ReferenceType.SHIPMENT,
                        title
                );

        if (alreadyNotified) {
            log.info("[NOTIF] Shipment status already notified shipmentId={} status={}",
                    shipmentId, event.status());
            return;
        }

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        senderId,
                        title,
                        message,
                        NotificationType.DELIVERY_STATUS,
                        shipmentId,
                        ReferenceType.SHIPMENT
                )
        );

        log.info("[NOTIF] Shipment notification sent shipmentId={} status={}",
                shipmentId, event.status());
    }

    private String buildTitle(ShipmentStatus status) {
        return switch (status) {
            case IN_TRANSIT -> "Shipment in transit";
            case DELIVERED -> "Shipment delivered";
            case CANCELLED -> "Shipment cancelled";
            case LOST -> "Shipment declared lost";
            default -> "Shipment update";
        };
    }

    private String buildMessage(ShipmentStatus status, UUID shipmentId) {
        return switch (status) {
            case IN_TRANSIT ->
                    "Your shipment " + shipmentId + " is now in transit.";
            case DELIVERED ->
                    "Your shipment " + shipmentId + " has been delivered successfully.";
             case LOST ->
                "Your shipment " + shipmentId + " has been declared lost. Please check your insurance claim status.";
            default ->
                    "Shipment updated.";
        };
    }
}
