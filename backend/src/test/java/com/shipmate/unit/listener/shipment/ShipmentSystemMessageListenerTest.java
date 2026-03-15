package com.shipmate.unit.listener.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.shipmate.listener.message.MessageSentEvent;
import com.shipmate.listener.shipment.ShipmentStatusChangedEvent;
import com.shipmate.listener.shipment.ShipmentSystemMessageListener;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.message.MessageRepository;
import com.shipmate.repository.shipment.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class ShipmentSystemMessageListenerTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ShipmentSystemMessageListener listener;

    @Test
    void onShipmentStatusChanged_shouldIgnoreUnsupportedStatus() {
        UUID shipmentId = UUID.randomUUID();
        when(shipmentRepository.findWithBookingAndSender(shipmentId)).thenReturn(Optional.of(shipment(shipmentId)));

        listener.onShipmentStatusChanged(new ShipmentStatusChangedEvent(shipmentId, ShipmentStatus.ASSIGNED));

        verify(messageRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onShipmentStatusChanged_shouldCreateSystemMessageAndPublishEvent() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = shipment(shipmentId);
        Message saved = Message.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .sender(shipment.getBooking().getDriver())
                .receiver(shipment.getSender())
                .messageType(MessageType.SYSTEM)
                .messageContent("Your shipment has been delivered")
                .build();

        when(shipmentRepository.findWithBookingAndSender(shipmentId)).thenReturn(Optional.of(shipment));
        when(messageRepository.save(org.mockito.ArgumentMatchers.any(Message.class))).thenReturn(saved);

        listener.onShipmentStatusChanged(new ShipmentStatusChangedEvent(shipmentId, ShipmentStatus.DELIVERED));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        assertThat(message.getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(message.getSender()).isEqualTo(shipment.getBooking().getDriver());
        assertThat(message.getReceiver()).isEqualTo(shipment.getSender());
        assertThat(message.getMessageContent()).isEqualTo("Your shipment has been delivered");
        assertThat(message.isRead()).isFalse();

        ArgumentCaptor<MessageSentEvent> eventCaptor = ArgumentCaptor.forClass(MessageSentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().messageId()).isEqualTo(saved.getId());
    }

    @Test
    void onShipmentStatusChanged_shouldCreateLostSystemMessage() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = shipment(shipmentId);
        when(shipmentRepository.findWithBookingAndSender(shipmentId)).thenReturn(Optional.of(shipment));
        when(messageRepository.save(org.mockito.ArgumentMatchers.any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Message.class));

        listener.onShipmentStatusChanged(new ShipmentStatusChangedEvent(shipmentId, ShipmentStatus.LOST));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getMessageContent()).contains("declared lost");
    }

    private Shipment shipment(UUID shipmentId) {
        User sender = User.builder()
                .id(UUID.randomUUID())
                .firstName("Sender")
                .lastName("User")
                .build();

        User driver = User.builder()
                .id(UUID.randomUUID())
                .firstName("Driver")
                .lastName("User")
                .build();

        Booking booking = Booking.builder()
                .driver(driver)
                .build();

        return Shipment.builder()
                .id(shipmentId)
                .sender(sender)
                .booking(booking)
                .status(ShipmentStatus.CREATED)
                .build();
    }
}
