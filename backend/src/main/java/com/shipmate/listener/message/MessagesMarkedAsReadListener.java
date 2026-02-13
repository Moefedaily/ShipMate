package com.shipmate.listener.message;


import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagesMarkedAsReadListener {

    private final MessageEventPublisher messageEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagesMarkedAsRead(MessagesMarkedAsReadEvent event) {

        if (event.bookingId() == null || event.userId() == null) {
            log.warn("[MSG] MessagesMarkedAsReadEvent ignored due to null values");
            return;
        }

        log.info(
            "[MSG] MessagesMarkedAsReadEvent fired bookingId={} userId={}",
            event.bookingId(),
            event.userId()
        );

        messageEventPublisher.publishConversationUpdate(
                event.bookingId(),
                event.userId()
        );
    }
}
