package com.shipmate.listener.booking;

import com.shipmate.listener.message.MessageSentEvent;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.message.MessageRepository;

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
public class BookingSystemMessageListener {

    private final BookingRepository bookingRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {

        log.info("[SYSMSG] Booking status changed bookingId={}, status={}",
                event.bookingId(), event.status());

        Booking booking = bookingRepository
                .findWithShipmentsById(event.bookingId())
                .orElseThrow();

        if (event.status() == BookingStatus.COMPLETED) {
            return;
        }

        for (Shipment shipment : booking.getShipments()) {

            User receiver = shipment.getSender();
            User driver = booking.getDriver();

            Message systemMessage = Message.builder()
                    .shipment(shipment)
                    .sender(driver)
                    .receiver(receiver)
                    .messageType(MessageType.SYSTEM)
                    .messageContent(buildContent(event.status()))
                    .isRead(false)
                    .build();

            Message saved = messageRepository.save(systemMessage);

            eventPublisher.publishEvent(
                    new MessageSentEvent(saved.getId())
            );

            log.info("[SYSMSG] Created system message for shipmentId={}",
                    shipment.getId());
        }
    }

    private String buildContent(BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> "Driver accepted your shipment";
            case IN_PROGRESS -> "Trip started";
            case CANCELLED -> "Booking was cancelled";
            default -> "Booking updated";
        };
    }
}
