package com.shipmate.repository.notification;

import com.shipmate.model.notification.Notification;
import com.shipmate.model.user.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndIsReadFalse(User user);

    Page<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
           set n.isRead = true
         where n.user = :user
           and n.isRead = false
    """)
    int markAllAsRead(@Param("user") User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
           set n.isRead = true
         where n.id = :notificationId
           and n.user = :user
           and n.isRead = false
    """)
    int markOneAsRead(
            @Param("notificationId") UUID notificationId,
            @Param("user") User user
    );
}
