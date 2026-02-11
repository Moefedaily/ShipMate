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

        Booking booking = bookingRepository
                .findWithShipmentsById(event.bookingId())
                .orElseThrow();

        List<User> recipients = resolveRecipients(booking, event.status());

        for (User recipient : recipients) {

            String title = "Booking update";
            String message = buildMessageForRecipient(event.status(), recipient, booking);

            eventPublisher.publishEvent(
            new NotificationRequestedEvent(
                    recipient.getId(),
                    title,
                    message,
                    NotificationType.BOOKING_UPDATE
            )
        );

        }
    }

    private List<User> resolveRecipients(Booking booking, BookingStatus status) {
        List<User> recipients = new ArrayList<>();

        User sender = resolveSender(booking);
        User driver = booking.getDriver();

        if (sender != null) recipients.add(sender);

        if (driver != null && (status == BookingStatus.CANCELLED || status == BookingStatus.COMPLETED)) {
            if (sender == null || !driver.getId().equals(sender.getId())) {
                recipients.add(driver);
            }
        }

        return recipients;
    }

    private String buildMessageForRecipient(BookingStatus status, User recipient, Booking booking) {
        return switch (status) {
            case CONFIRMED -> "Your booking has been confirmed by the driver.";
            case IN_PROGRESS -> "Your delivery has started.";
            case COMPLETED -> "Your delivery has been completed.";
            case CANCELLED -> "The booking was cancelled.";
            default -> "Your booking status has been updated.";
        };
    }

    private User resolveSender(Booking booking) {
        return booking.getShipments()
                .get(0)
                .getSender();
    }
}
