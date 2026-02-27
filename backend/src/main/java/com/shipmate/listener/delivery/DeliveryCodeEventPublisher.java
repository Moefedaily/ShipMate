package com.shipmate.listener.delivery;

import com.shipmate.dto.ws.delivery.DeliveryCodeWsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryCodeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishToSender(UUID senderId, UUID shipmentId, String code) {
    log.info("[DELIVERY_CODE] WS push to senderId={} shipmentId={}", senderId, shipmentId);
        DeliveryCodeWsDto payload =
                new DeliveryCodeWsDto(shipmentId, code);

        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/delivery-code",
                payload
        );
    }
}
