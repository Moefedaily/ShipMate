package com.shipmate.listener.booking;

import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.message.MessageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class BookingSystemMessageListener {

    private final BookingRepository bookingRepository;
    private final MessageRepository messageRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {

        Booking booking = bookingRepository
                .findWithShipmentsById(event.bookingId())
                .orElseThrow();

        Message systemMessage = Message.builder()
                .booking(booking)
                .sender(booking.getDriver())
                .receiver(resolveReceiver(booking))
                .messageType(MessageType.SYSTEM)
                .messageContent(buildContent(event.status()))
                .build();

        messageRepository.save(systemMessage);
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
