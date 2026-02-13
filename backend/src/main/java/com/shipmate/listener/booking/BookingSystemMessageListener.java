package com.shipmate.listener.booking;

import com.shipmate.listener.message.MessageSentEvent;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.message.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

        log.info("[SYSMSG] listener fired bookingId={}, status={}", event.bookingId(), event.status());

        Booking booking = bookingRepository
                .findWithShipmentsById(event.bookingId())
                .orElseThrow();

        User receiver = resolveReceiver(booking);

        Message systemMessage = Message.builder()
                .booking(booking)
                .sender(booking.getDriver())
                .receiver(receiver)
                .messageType(MessageType.SYSTEM)
                .messageContent(buildContent(event.status()))
                .isRead(false)
                .build();

        Message saved = messageRepository.saveAndFlush(systemMessage);

        if (saved.getId() == null) {
            log.error("[SYSMSG] saveAndFlush returned null id. bookingId={}", booking.getId());
            return;
        }

        log.info("[SYSMSG] saved system message id={} bookingId={}", saved.getId(), booking.getId());

        eventPublisher.publishEvent(new MessageSentEvent(saved.getId()));
    }

    private String buildContent(BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> "Driver accepted the booking";
            case IN_PROGRESS -> "Delivery started";
            case COMPLETED -> "Delivery completed";
            case CANCELLED -> "Booking was cancelled";
            default -> "Booking updated";
        };
    }

    private User resolveReceiver(Booking booking) {
        return booking.getShipments()
                .get(0)
                .getSender();
    }
}
