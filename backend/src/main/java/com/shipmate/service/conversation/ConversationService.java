package com.shipmate.service.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shipmate.dto.response.conversation.ConversationResponse;
import com.shipmate.model.booking.Booking;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.message.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConversationService {

    private final BookingRepository bookingRepository;
    private final MessageRepository messageRepository;

    public List<ConversationResponse> getMyConversations(UUID userId) {

        List<Booking> bookings =
            bookingRepository.findAllUserBookings(userId);
        log.info("Found {} bookings for user {}", bookings.size(), userId);    

        return bookings.stream()
            .map(b -> toConversation(b, userId))
            .toList();
    }

    private ConversationResponse toConversation(Booking booking, UUID userId) {

        var lastMessage =
            messageRepository.findLatestByBooking(
                booking.getId(),
                PageRequest.of(0, 1)
            ).stream().findFirst().orElse(null);

        long unread =
            messageRepository
                .countByBooking_IdAndReceiver_IdAndIsReadFalse(
                    booking.getId(),
                    userId
                );

        return new ConversationResponse(
            booking.getId(),
            booking.getStatus(),
            lastMessage != null ? lastMessage.getMessageContent() : null,
            lastMessage != null ? lastMessage.getSentAt() : null,
            unread
        );
    }
}
