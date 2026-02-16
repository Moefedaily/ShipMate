package com.shipmate.listener.shipment;

import com.shipmate.listener.message.MessageSentEvent;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.repository.message.MessageRepository;
import com.shipmate.repository.shipment.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentSystemMessageListener {

    private final ShipmentRepository shipmentRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentStatusChanged(ShipmentStatusChangedEvent event) {

        Shipment shipment = shipmentRepository
                .findWithBookingAndSender(event.shipmentId())
                .orElseThrow();

        if (event.status() != ShipmentStatus.IN_TRANSIT &&
            event.status() != ShipmentStatus.DELIVERED &&
            event.status() != ShipmentStatus.CANCELLED) {
            return;
        }

        Message systemMessage = Message.builder()
                .shipment(shipment)
                .sender(shipment.getBooking().getDriver())
                .receiver(shipment.getSender())
                .messageType(MessageType.SYSTEM)
                .messageContent(buildContent(event.status()))
                .isRead(false)
                .build();

        Message saved = messageRepository.save(systemMessage);

        eventPublisher.publishEvent(
                new MessageSentEvent(saved.getId())
        );

        log.info("[SYSMSG] Shipment system message created shipmentId={} status={}",
                shipment.getId(), event.status());
    }

    private String buildContent(ShipmentStatus status) {
        return switch (status) {
            case IN_TRANSIT -> "Your shipment is now in transit";
            case DELIVERED -> "Your shipment has been delivered";
            case CANCELLED -> "Your shipment was cancelled";
            default -> "Shipment updated";
        };
    }
}
