package com.shipmate.listener.message;

import com.shipmate.model.message.Message;
import com.shipmate.repository.message.MessageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class MessageSentListener {

    private final MessageRepository messageRepository;
    private final MessageEventPublisher messageEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        Message message =
                messageRepository.findWithRelationsById(event.messageId());

        messageEventPublisher.messageSent(message);
    }
}
