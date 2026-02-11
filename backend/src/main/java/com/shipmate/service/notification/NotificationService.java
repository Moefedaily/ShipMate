package com.shipmate.service.notification;

import com.shipmate.dto.response.notification.NotificationResponse;
import com.shipmate.dto.ws.notification.UnreadCountWsDto;
import com.shipmate.mapper.notification.NotificationMapper;
import com.shipmate.mapper.ws.NotificationWsMapper;
import com.shipmate.model.notification.Notification;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.user.User;
import com.shipmate.repository.notification.NotificationRepository;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(
            UUID userId,
            boolean unreadOnly,
            Pageable pageable
    ) {
        User user = loadUser(userId);

        return unreadOnly
                ? notificationRepository
                    .findByUserAndIsReadFalseOrderByCreatedAtDesc(user, pageable)
                    .map(notificationMapper::toResponse)
                : notificationRepository
                    .findByUserOrderByCreatedAtDesc(user, pageable)
                    .map(notificationMapper::toResponse);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createAndDispatch(
            UUID recipientUserId,
            String title,
            String message,
            NotificationType type
    ) {
        User recipient = loadUser(recipientUserId);

        Notification notification = Notification.builder()
                .user(recipient)
                .title(title)
                .message(message)
                .notificationType(type)
                .build();

        notificationRepository.saveAndFlush(notification);

        long unreadCount =
                notificationRepository.countByUserAndIsReadFalse(recipient);

        messagingTemplate.convertAndSend(
                "/topic/users/" + recipient.getId() + "/notifications",
                NotificationWsMapper.toWsDto(notification)
        );

        messagingTemplate.convertAndSend(
                "/topic/users/" + recipient.getId() + "/notifications/unread-count",
                new UnreadCountWsDto(unreadCount)
        );

        return notification;
    }
    @Transactional
    public long markAllAsRead(UUID userId) {
        User user = loadUser(userId);

        notificationRepository.markAllAsRead(user);

        long unreadCount =
                notificationRepository.countByUserAndIsReadFalse(user);

        messagingTemplate.convertAndSend(
                "/topic/users/" + user.getId() + "/notifications/unread-count",
                new UnreadCountWsDto(unreadCount)
        );

        return unreadCount;
    }

    @Transactional
    public long markOneAsRead(UUID userId, UUID notificationId) {
        User user = loadUser(userId);

        notificationRepository.markOneAsRead(notificationId, user);

        long unreadCount =
                notificationRepository.countByUserAndIsReadFalse(user);

        messagingTemplate.convertAndSend(
                "/topic/users/" + user.getId() + "/notifications/unread-count",
                new UnreadCountWsDto(unreadCount)
        );

        return unreadCount;
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        User user = loadUser(userId);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

 }
