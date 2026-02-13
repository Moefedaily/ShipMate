package com.shipmate.listener.message;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.shipmate.dto.response.conversation.ConversationResponse;
import com.shipmate.mapper.message.MessageMapper;
import com.shipmate.model.message.Message;
import com.shipmate.repository.message.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageMapper messageMapper;
    private final MessageRepository messageRepository;

    public void messageSent(Message message) {
        messagingTemplate.convertAndSend(
            "/topic/bookings/" + message.getBooking().getId() + "/messages",
            messageMapper.toResponse(message)
        );

        conversationUpdated(message);
    }

    public void conversationUpdated(Message message) {

        UUID bookingId = message.getBooking().getId();
        UUID senderId = message.getSender().getId();
        UUID receiverId = message.getReceiver().getId();

        publishConversationUpdate(bookingId, senderId);
        publishConversationUpdate(bookingId, receiverId);
    }

    public void publishConversationUpdate(UUID bookingId, UUID userId) {

        long unreadCount =
            messageRepository.countByBooking_IdAndReceiver_IdAndIsReadFalse(
                    bookingId,
                    userId
            );

        Message lastMessage =
            messageRepository.findLatestByBooking(
                    bookingId,
                    PageRequest.of(0, 1)
            ).stream().findFirst().orElse(null);

        if (lastMessage == null) {
            log.debug("[MSG] No last message found for bookingId={}", bookingId);
            return;
        }

        ConversationResponse payload =
            new ConversationResponse(
                    bookingId,
                    lastMessage.getBooking().getStatus(),
                    lastMessage.getMessageContent(),
                    lastMessage.getSentAt(),
                    unreadCount
            );

        messagingTemplate.convertAndSend(
                "/topic/users/" + userId + "/conversation-updates",
                payload
        );

        log.info(
            "[MSG] Conversation update pushed bookingId={} userId={} unread={}",
            bookingId,
            userId,
            unreadCount
        );
    }
}
