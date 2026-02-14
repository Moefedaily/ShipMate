package com.shipmate.controller.conversation;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.shipmate.dto.ws.typing.TypingWsDto;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TypingController {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/shipments/{shipmentId}/typing")
    public void typing(
            @DestinationVariable UUID shipmentId,
            Principal principal
    ) {

        UUID userId = UUID.fromString(principal.getName());

        var user = userRepository.findById(userId)
                .orElseThrow();

        TypingWsDto payload =
                new TypingWsDto(
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName()
                );

        messagingTemplate.convertAndSend(
                "/topic/shipments/" + shipmentId + "/typing",
                payload
        );
    }
}
