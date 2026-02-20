package com.shipmate.listener.delivery;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryUnlockedListener {

    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryUnlocked(DeliveryUnlockedEvent event) {

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        event.senderId(),
                        "Delivery Reopened",
                        "Delivery confirmation has been unlocked by support.",
                        NotificationType.SYSTEM_ALERT,
                        event.shipmentId(),
                        ReferenceType.SHIPMENT
                )
        );

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        event.driverId(),
                        "Delivery Unlocked",
                        "You may now retry delivery confirmation.",
                        NotificationType.SYSTEM_ALERT,
                        event.shipmentId(),
                        ReferenceType.SHIPMENT
                )
        );

        log.info("[DELIVERY] Unlock notifications sent shipmentId={}",
                event.shipmentId());
    }
}
