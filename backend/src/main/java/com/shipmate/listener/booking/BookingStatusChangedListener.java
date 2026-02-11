package com.shipmate.listener.booking;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class BookingStatusChangedListener {

    private final BookingEventPublisher bookingEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        bookingEventPublisher.bookingUpdated(event.bookingId(), event.status());
    }
}
