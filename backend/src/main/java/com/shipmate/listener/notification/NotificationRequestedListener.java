package com.shipmate.listener.notification;

import com.shipmate.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestedListener {

    private final NotificationService notificationService;

    @EventListener
    public void onNotificationRequested(NotificationRequestedEvent event) {

        notificationService.createAndDispatch(
                event.recipientUserId(),
                event.title(),
                event.message(),
                event.type()
        );
    }
}
