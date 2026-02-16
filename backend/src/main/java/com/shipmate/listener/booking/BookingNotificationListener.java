package com.shipmate.listener.booking;

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationListener {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {

        if (event.status() != BookingStatus.CONFIRMED &&
            event.status() != BookingStatus.CANCELLED){
            return;
        }

        Booking booking = bookingRepository
                .findWithShipmentsById(event.bookingId())
                .orElseThrow();

        List<User> recipients = resolveAllParticipants(booking);

        for (User recipient : recipients) {

            if (recipient.getId().equals(event.actorId())) {
                continue;
            }

            eventPublisher.publishEvent(
                    new NotificationRequestedEvent(
                            recipient.getId(),
                            "Trip update",
                            buildMessage(event.status()),
                            NotificationType.BOOKING_UPDATE
                    )
            );
        }
    }

    private List<User> resolveAllParticipants(Booking booking) {

        List<User> recipients = new ArrayList<>();

        if (booking.getDriver() != null) {
            recipients.add(booking.getDriver());
        }

        booking.getShipments().stream()
                .map(shipment -> shipment.getSender())
                .distinct()
                .forEach(recipients::add);

        return recipients;
    }

    private String buildMessage(BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> "The trip has been confirmed by the driver.";
            case CANCELLED -> "The trip was cancelled.";
            default -> "Trip updated.";
        };
    }
}
