package com.shipmate.listener.booking;

import com.shipmate.dto.ws.booking.BookingStatusUpdateWsDto;
import com.shipmate.model.booking.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void bookingUpdated(UUID bookingId, BookingStatus status) {

        BookingStatusUpdateWsDto payload =
                new BookingStatusUpdateWsDto(bookingId, status);

        messagingTemplate.convertAndSend(
                "/topic/bookings/" + bookingId,
                payload
        );
    }
}
