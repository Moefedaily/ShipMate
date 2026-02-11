package com.shipmate.listener.message;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.shipmate.mapper.message.MessageMapper;
import com.shipmate.model.message.Message;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessageEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageMapper messageMapper;

    public void messageSent(Message message) {
        messagingTemplate.convertAndSend(
                "/topic/bookings/" + message.getBooking().getId() + "/messages",
                messageMapper.toResponse(message)
        );
        messagingTemplate.convertAndSendToUser(
                message.getReceiver().getId().toString(),
                "/queue/messages",
                messageMapper.toResponse(message)
        );

    }
}

