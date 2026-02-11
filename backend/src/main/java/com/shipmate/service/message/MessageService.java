package com.shipmate.service.message;

import com.shipmate.dto.request.message.SendMessageRequest;
import com.shipmate.dto.response.message.MessageResponse;
import com.shipmate.listener.message.MessageSentEvent;
import com.shipmate.mapper.message.MessageMapper;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.message.MessageRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final ShipmentRepository shipmentRepository;
    private final MessageMapper messageMapper;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;



    @Transactional(readOnly = true)
    public Page<MessageResponse> getBookingMessages(
            UUID bookingId,
            UUID userId,
            Pageable pageable
    ) {
        Booking booking = loadBooking(bookingId);

        validateAccess(booking, userId);

        return messageRepository
                .findByBooking_IdOrderBySentAtAsc(bookingId, pageable)
                .map(messageMapper::toResponse);
    }


    private void validateAccess(Booking booking, UUID userId) {

        if (booking.getDriver() != null &&
            booking.getDriver().getId().equals(userId)) {
            return;
        }

        boolean isSenderInBooking =
                shipmentRepository.existsByBooking_IdAndSender_Id(
                        booking.getId(),
                        userId
                );

        if (isSenderInBooking) {
            return;
        }

        throw new AccessDeniedException(
                "You are not allowed to access messages for this booking"
        );
    }


    private Booking loadBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found")
                );
    }

    public MessageResponse sendMessage(
        UUID bookingId,
        UUID senderId,
        SendMessageRequest request
) {
    Booking booking = loadBooking(bookingId);

    validateAccess(booking, senderId);

    UUID receiverId = resolveReceiver(booking, senderId);

    Message message = Message.builder()
            .booking(booking)
            .sender(userRepository.getReferenceById(senderId))
            .receiver(userRepository.getReferenceById(receiverId))
            .messageContent(request.message())
            .messageType(MessageType.TEXT)
            .isRead(false)
            .build();

    Message saved = messageRepository.save(message);

    eventPublisher.publishEvent(
            new MessageSentEvent(saved.getId())
    );


    return messageMapper.toResponse(saved);
}

private UUID resolveReceiver(Booking booking, UUID senderId) {

    if (booking.getDriver() != null &&
        booking.getDriver().getId().equals(senderId)) {

        return booking.getShipments()
                .get(0)
                .getSender()
                .getId();
    }

    if (booking.getDriver() != null) {
        return booking.getDriver().getId();
    }

    throw new IllegalStateException("Cannot resolve message receiver");
}

public void markMessagesAsRead(UUID bookingId, UUID userId) {
    Booking booking = loadBooking(bookingId);
    validateAccess(booking, userId);

    messageRepository.markAllAsRead(bookingId, userId);
}

}
