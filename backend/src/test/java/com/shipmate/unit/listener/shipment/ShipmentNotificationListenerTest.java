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

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.listener.shipment.ShipmentNotificationListener;
import com.shipmate.listener.shipment.ShipmentStatusChangedEvent;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.notification.NotificationRepository;
import com.shipmate.repository.shipment.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class ShipmentNotificationListenerTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ShipmentNotificationListener listener;

    @Test
    void onShipmentStatusChanged_shouldIgnoreUnsupportedStatuses() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = shipment(shipmentId);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        listener.onShipmentStatusChanged(new ShipmentStatusChangedEvent(shipmentId, ShipmentStatus.ASSIGNED));

        verify(notificationRepository, never())
                .existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onShipmentStatusChanged_shouldSkipDuplicateNotification() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = shipment(shipmentId);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(notificationRepository.existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                shipment.getSender().getId(),
                shipmentId,
                ReferenceType.SHIPMENT,
                "Shipment in transit"
        )).thenReturn(true);

        listener.onShipmentStatusChanged(new ShipmentStatusChangedEvent(shipmentId, ShipmentStatus.IN_TRANSIT));

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onShipmentStatusChanged_shouldPublishNotificationForLostShipment() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = shipment(shipmentId);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(notificationRepository.existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                shipment.getSender().getId(),
                shipmentId,
                ReferenceType.SHIPMENT,
                "Shipment declared lost"
        )).thenReturn(false);

        listener.onShipmentStatusChanged(new ShipmentStatusChangedEvent(shipmentId, ShipmentStatus.LOST));

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationRequestedEvent event = captor.getValue();
        assertThat(event.recipientUserId()).isEqualTo(shipment.getSender().getId());
        assertThat(event.referenceId()).isEqualTo(shipmentId);
        assertThat(event.referenceType()).isEqualTo(ReferenceType.SHIPMENT);
        assertThat(event.title()).isEqualTo("Shipment declared lost");
        assertThat(event.message()).contains("declared lost");
    }

    private Shipment shipment(UUID shipmentId) {
        User sender = User.builder()
                .id(UUID.randomUUID())
                .firstName("Sender")
                .lastName("User")
                .build();

        return Shipment.builder()
                .id(shipmentId)
                .sender(sender)
                .status(ShipmentStatus.CREATED)
                .build();
    }
}
