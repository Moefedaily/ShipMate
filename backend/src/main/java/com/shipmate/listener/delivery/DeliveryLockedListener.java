package com.shipmate.listener.delivery;

import com.shipmate.dto.ws.shipment.ShipmentUpdateWsDto;
import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.model.shipment.ShipmentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryLockedListener {

    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onDeliveryLocked(DeliveryLockedEvent event) {

            messagingTemplate.convertAndSend(
                    "/topic/shipments/" + event.shipmentId(),
                    new ShipmentUpdateWsDto(
                            event.shipmentId(),
                            ShipmentStatus.IN_TRANSIT,
                            true
                    )
            );

            eventPublisher.publishEvent(
                    new NotificationRequestedEvent(
                            event.senderId(),
                            "Delivery Issue Detected",
                            "Delivery confirmation failed multiple times. Please contact support.",
                            NotificationType.SYSTEM_ALERT,
                            event.shipmentId(),
                            ReferenceType.SHIPMENT
                    )
            );

            eventPublisher.publishEvent(
                    new NotificationRequestedEvent(
                            event.driverId(),
                            "Delivery Locked",
                            "Maximum delivery code attempts reached. Please cancel the shipment or contact support.",
                            NotificationType.SYSTEM_ALERT,
                            event.bookingId(),
                            ReferenceType.BOOKING
                    )
            );

            log.info("[DELIVERY] Shipment locked shipmentId={}", event.shipmentId());
        }
    }