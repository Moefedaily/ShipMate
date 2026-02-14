package com.shipmate.service.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shipmate.dto.response.conversation.ConversationResponse;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.repository.message.MessageRepository;
import com.shipmate.repository.shipment.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConversationService {

    private final ShipmentRepository shipmentRepository;
    private final MessageRepository messageRepository;

    public List<ConversationResponse> getMyConversations(UUID userId) {

        List<Shipment> shipments =
                shipmentRepository.findAllUserShipments(userId);

        log.info("Found {} shipments for user {}", shipments.size(), userId);

        return shipments.stream()
                .map(shipment -> toConversation(shipment, userId))
                .toList();
    }

    private ConversationResponse toConversation(Shipment shipment, UUID userId) {

        var lastMessage =
                messageRepository.findLatestByShipment(
                        shipment.getId(),
                        PageRequest.of(0, 1)
                ).stream().findFirst().orElse(null);

        long unread =
                messageRepository.countByShipment_IdAndReceiver_IdAndIsReadFalse(
                        shipment.getId(),
                        userId
                );

        return new ConversationResponse(
                shipment.getId(),
                shipment.getStatus(),
                lastMessage != null ? lastMessage.getMessageContent() : null,
                lastMessage != null ? lastMessage.getSentAt() : null,
                unread
        );
    }
}
