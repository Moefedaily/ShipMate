package com.shipmate.integration.notification;

import static org.assertj.core.api.Assertions.*;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.notification.Notification;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.notification.NotificationRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.notification.NotificationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

class NotificationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    void getMyNotifications_shouldReturnOnlyMine() {
        User u1 = createUser();
        User u2 = createUser();

        createNotification(u1, false);
        createNotification(u1, true);
        createNotification(u2, false);

        var page = notificationService.getMyNotifications(
                u1.getId(),
                false,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allMatch(n -> !n.isRead() || n.isRead());
    }

    @Test
    void getMyNotifications_unreadOnly_shouldReturnOnlyUnread() {
        User user = createUser();

        createNotification(user, false);
        createNotification(user, true);
        createNotification(user, false);

        var page = notificationService.getMyNotifications(
                user.getId(),
                true,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allMatch(n -> !n.isRead());
    }

    @Test
    void countUnread_shouldReturnCorrectValue() {
        User user = createUser();

        createNotification(user, false);
        createNotification(user, false);
        createNotification(user, true);

        long count = notificationService.countUnread(user.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void markAllAsRead_shouldMarkOnlyMyUnread() {
        User user = createUser();

        createNotification(user, false);
        createNotification(user, false);
        createNotification(user, true);

        notificationService.markAllAsRead(user.getId());

        long unread = notificationService.countUnread(user.getId());
        assertThat(unread).isZero();
    }

    @Test
    void markAllAsRead_shouldNotAffectOtherUsers() {
        User u1 = createUser();
        User u2 = createUser();

        createNotification(u1, false);
        createNotification(u2, false);

        notificationService.markAllAsRead(u1.getId());

        assertThat(notificationService.countUnread(u1.getId())).isZero();
        assertThat(notificationService.countUnread(u2.getId())).isEqualTo(1);
    }

    
    private User createUser() {
        return userRepository.save(
                User.builder()
                        .email("user-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }

    private Notification createNotification(User user, boolean read) {
        return notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .title("Test notification")
                        .message("Test message")
                        .notificationType(NotificationType.BOOKING_UPDATE)
                        .isRead(read)
                        .createdAt(Instant.now())
                        .build()
        );
    }
}
