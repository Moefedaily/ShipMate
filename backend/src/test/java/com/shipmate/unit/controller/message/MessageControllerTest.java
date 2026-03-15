package com.shipmate.unit.controller.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.shipmate.controller.message.MessageController;
import com.shipmate.dto.request.message.SendMessageRequest;
import com.shipmate.dto.response.message.MessageResponse;
import com.shipmate.model.message.MessageType;
import com.shipmate.service.message.MessageService;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Test
    void getShipmentMessages_shouldReturnPagedMessages() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MessageController controller = new MessageController(messageService);

        MessageResponse message = new MessageResponse(
                UUID.randomUUID(),
                shipmentId,
                MessageType.TEXT,
                "hello",
                userId,
                UUID.randomUUID(),
                false,
                Instant.parse("2026-03-15T10:15:30Z")
        );

        when(messageService.getShipmentMessages(eq(shipmentId), eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(message), PageRequest.of(0, 20), 1));

        var response = controller.getShipmentMessages(shipmentId, userId.toString(), PageRequest.of(0, 20));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().getFirst().messageContent()).isEqualTo("hello");
    }

    @Test
    void markAsRead_shouldReturnNoContent() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MessageController controller = new MessageController(messageService);

        var response = controller.markAsRead(shipmentId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(messageService).markMessagesAsRead(shipmentId, userId);
    }

    @Test
    void sendMessage_shouldDelegateToServiceAndReturnPayload() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MessageController controller = new MessageController(messageService);
        MessageResponse response = new MessageResponse(
                UUID.randomUUID(),
                shipmentId,
                MessageType.TEXT,
                "Need an update",
                userId,
                UUID.randomUUID(),
                false,
                Instant.parse("2026-03-15T11:00:00Z")
        );

        when(messageService.sendMessage(eq(shipmentId), eq(userId), any()))
                .thenReturn(response);

        var result = controller.sendMessage(shipmentId, userId.toString(), new SendMessageRequest("Need an update"));

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().messageContent()).isEqualTo("Need an update");
        assertThat(result.getBody().messageType()).isEqualTo(MessageType.TEXT);
    }
}
