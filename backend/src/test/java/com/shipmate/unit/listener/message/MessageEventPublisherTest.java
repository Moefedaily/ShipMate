package com.shipmate.unit.listener.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.shipmate.dto.response.conversation.ConversationResponse;
import com.shipmate.dto.response.message.MessageResponse;
import com.shipmate.dto.response.photo.PhotoResponse;
import com.shipmate.listener.message.MessageEventPublisher;
import com.shipmate.mapper.message.MessageMapper;
import com.shipmate.mapper.photo.PhotoMapper;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.photo.Photo;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.message.MessageRepository;

@ExtendWith(MockitoExtension.class)
class MessageEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private PhotoMapper photoMapper;

    @InjectMocks
    private MessageEventPublisher publisher;

    @Test
    void messageSent_shouldPublishMessageAndConversationUpdates() {
        Message message = message();
        MessageResponse messageResponse = new MessageResponse(
                UUID.randomUUID(),
                message.getShipment().getId(),
                MessageType.TEXT,
                "hello",
                message.getSender().getId(),
                message.getReceiver().getId(),
                false,
                Instant.now()
        );

        when(messageMapper.toResponse(message)).thenReturn(messageResponse);
        when(messageRepository.countByShipment_IdAndReceiver_IdAndIsReadFalse(message.getShipment().getId(), message.getSender().getId())).thenReturn(0L);
        when(messageRepository.countByShipment_IdAndReceiver_IdAndIsReadFalse(message.getShipment().getId(), message.getReceiver().getId())).thenReturn(2L);
        when(messageRepository.findLatestByShipment(eq(message.getShipment().getId()), any()))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(photoMapper.toResponse(message.getReceiver().getAvatar()))
                .thenReturn(PhotoResponse.builder().id(UUID.randomUUID()).url("receiver-avatar").build());
        when(photoMapper.toResponse(message.getSender().getAvatar()))
                .thenReturn(PhotoResponse.builder().id(UUID.randomUUID()).url("sender-avatar").build());

        publisher.messageSent(message);

        verify(messagingTemplate).convertAndSend("/topic/shipments/" + message.getShipment().getId() + "/messages", messageResponse);
        verify(messagingTemplate).convertAndSend(eq("/topic/users/" + message.getSender().getId() + "/conversation-updates"), any(ConversationResponse.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/users/" + message.getReceiver().getId() + "/conversation-updates"), any(ConversationResponse.class));
    }

    @Test
    void publishConversationUpdate_shouldSkipWhenNoLastMessageExists() {
        UUID shipmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageRepository.countByShipment_IdAndReceiver_IdAndIsReadFalse(shipmentId, userId)).thenReturn(0L);
        when(messageRepository.findLatestByShipment(eq(shipmentId), any()))
                .thenReturn(new PageImpl<>(List.of()));

        publisher.publishConversationUpdate(shipmentId, userId);

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/users/" + userId + "/conversation-updates"), any(ConversationResponse.class));
    }

    @Test
    void publishConversationUpdate_shouldUseDriverAsOtherUserForSenderView() {
        Message message = message();
        UUID senderId = message.getReceiver().getId();
        PhotoResponse photoResponse = PhotoResponse.builder().id(UUID.randomUUID()).url("driver-avatar").build();

        when(messageRepository.countByShipment_IdAndReceiver_IdAndIsReadFalse(message.getShipment().getId(), senderId)).thenReturn(1L);
        when(messageRepository.findLatestByShipment(eq(message.getShipment().getId()), any()))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(photoMapper.toResponse(message.getSender().getAvatar())).thenReturn(photoResponse);

        publisher.publishConversationUpdate(message.getShipment().getId(), senderId);

        ArgumentCaptor<ConversationResponse> payloadCaptor = ArgumentCaptor.forClass(ConversationResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/users/" + senderId + "/conversation-updates"), payloadCaptor.capture());

        ConversationResponse payload = payloadCaptor.getValue();
        assertThat(payload.shipmentId()).isEqualTo(message.getShipment().getId());
        assertThat(payload.unreadCount()).isEqualTo(1L);
        assertThat(payload.otherUserName()).isEqualTo("Driver User");
        assertThat(payload.otherUserAvatar()).isEqualTo(photoResponse);
        assertThat(payload.lastMessagePreview()).isEqualTo("hello");
        assertThat(payload.shipmentStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
    }

    private Message message() {
        Photo senderAvatar = Photo.builder().id(UUID.randomUUID()).url("sender-avatar").build();
        Photo receiverAvatar = Photo.builder().id(UUID.randomUUID()).url("receiver-avatar").build();

        User driver = User.builder()
                .id(UUID.randomUUID())
                .firstName("Driver")
                .lastName("User")
                .avatar(senderAvatar)
                .build();

        User sender = User.builder()
                .id(UUID.randomUUID())
                .firstName("Sender")
                .lastName("User")
                .avatar(receiverAvatar)
                .build();

        Booking booking = Booking.builder()
                .driver(driver)
                .build();

        Shipment shipment = Shipment.builder()
                .id(UUID.randomUUID())
                .status(ShipmentStatus.ASSIGNED)
                .sender(sender)
                .booking(booking)
                .build();

        return Message.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .sender(driver)
                .receiver(sender)
                .messageType(MessageType.TEXT)
                .messageContent("hello")
                .sentAt(Instant.parse("2026-03-15T12:00:00Z"))
                .isRead(false)
                .build();
    }
}
