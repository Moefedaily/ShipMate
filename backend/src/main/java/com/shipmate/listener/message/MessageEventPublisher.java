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

        UUID shipmentId = message.getShipment().getId();

        messagingTemplate.convertAndSend(
                "/topic/shipments/" + shipmentId + "/messages",
                messageMapper.toResponse(message)
        );

        conversationUpdated(message);
    }


    public void conversationUpdated(Message message) {

        UUID shipmentId = message.getShipment().getId();
        UUID senderId = message.getSender().getId();
        UUID receiverId = message.getReceiver().getId();

        publishConversationUpdate(shipmentId, senderId);
        publishConversationUpdate(shipmentId, receiverId);
    }

    public void publishConversationUpdate(UUID shipmentId, UUID userId) {

        long unreadCount =
                messageRepository.countByShipment_IdAndReceiver_IdAndIsReadFalse(
                        shipmentId,
                        userId
                );

        Message lastMessage =
                messageRepository.findLatestByShipment(
                        shipmentId,
                        PageRequest.of(0, 1)
                ).stream().findFirst().orElse(null);

        if (lastMessage == null) {
            log.debug("[MSG] No last message found for shipmentId={}", shipmentId);
            return;
        }

        ConversationResponse payload =
                new ConversationResponse(
                        shipmentId,
                        lastMessage.getShipment().getStatus(),
                        lastMessage.getMessageContent(),
                        lastMessage.getSentAt(),
                        unreadCount
                );

        messagingTemplate.convertAndSend(
                "/topic/users/" + userId + "/conversation-updates",
                payload
        );

        log.info(
                "[MSG] Conversation update pushed shipmentId={} userId={} unread={}",
                shipmentId,
                userId,
                unreadCount
        );
    }
}
