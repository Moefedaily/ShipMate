package com.shipmate.listener.message;

import com.shipmate.model.message.Message;
import com.shipmate.repository.message.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSentListener {

    private final MessageRepository messageRepository;
    private final MessageEventPublisher messageEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        if (event.messageId() == null) {
            log.warn("[MSG] MessageSentEvent with null id ignored");
            return;
        }

        Message message = messageRepository.findWithRelationsById(event.messageId());
        log.info("[MSG] MessageSentEvent fired for id={}", event.messageId());

        messageEventPublisher.messageSent(message);
    }

}
