package com.shipmate.service.message;

import com.shipmate.dto.request.message.SendMessageRequest;
import com.shipmate.dto.response.message.MessageResponse;
import com.shipmate.listener.message.MessageSentEvent;
import com.shipmate.listener.message.MessagesMarkedAsReadEvent;
import com.shipmate.mapper.message.MessageMapper;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
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
    private final ShipmentRepository shipmentRepository;
    private final MessageMapper messageMapper;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    


    @Transactional(readOnly = true)
    public Page<MessageResponse> getShipmentMessages(
            UUID shipmentId,
            UUID userId,
            Pageable pageable
    ) {

        Shipment shipment = loadShipment(shipmentId);

        validateAccess(shipment, userId);

        return messageRepository
                .findByShipment_IdOrderBySentAtAsc(shipmentId, pageable)
                .map(messageMapper::toResponse);
    }

    public MessageResponse sendMessage(
            UUID shipmentId,
            UUID senderId,
            SendMessageRequest request
    ) {

        Shipment shipment = loadShipment(shipmentId);

        validateAccess(shipment, senderId);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED ||
            shipment.getStatus() == ShipmentStatus.CANCELLED) {

            throw new IllegalStateException("Chat is closed for this shipment");
        }

        UUID receiverId = resolveReceiver(shipment, senderId);

        Message message = Message.builder()
                .shipment(shipment)
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


    public void markMessagesAsRead(UUID shipmentId, UUID userId) {

        Shipment shipment = loadShipment(shipmentId);

        validateAccess(shipment, userId);

        messageRepository.markAllAsRead(shipmentId, userId);

        eventPublisher.publishEvent(
                new MessagesMarkedAsReadEvent(shipmentId, userId)
        );
    }


    private Shipment loadShipment(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Shipment not found")
                );
    }

    private void validateAccess(Shipment shipment, UUID userId) {

        if (shipment.getBooking().getDriver() != null &&
            shipment.getBooking().getDriver().getId().equals(userId)) {
            return;
        }

        if (shipment.getSender().getId().equals(userId)) {
            return;
        }

        throw new AccessDeniedException(
                "You are not allowed to access messages for this shipment"
        );
    }

    private UUID resolveReceiver(Shipment shipment, UUID senderId) {

        if (shipment.getBooking() == null ||
            shipment.getBooking().getDriver() == null) {

            throw new IllegalStateException("Shipment is not assigned to a driver yet");
        }

        UUID driverId = shipment.getBooking().getDriver().getId();
        UUID senderShipmentId = shipment.getSender().getId();

        if (senderId.equals(driverId)) {
            return senderShipmentId;
        }

        if (senderId.equals(senderShipmentId)) {
            return driverId;
        }

        throw new IllegalStateException("Cannot resolve message receiver");
    }

}
