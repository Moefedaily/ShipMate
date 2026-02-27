package com.shipmate.controller.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.dto.response.conversation.ConversationResponse;
import com.shipmate.service.conversation.ConversationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/me")
    public List<ConversationResponse> getMyConversations(
        @AuthenticationPrincipal(expression = "username") String userId
    ) {
        return conversationService.getMyConversations(
            UUID.fromString(userId)
        );
    }
}
