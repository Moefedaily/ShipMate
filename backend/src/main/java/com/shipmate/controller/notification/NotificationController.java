package com.shipmate.controller.notification;

import com.shipmate.dto.response.notification.NotificationResponse;
import com.shipmate.service.notification.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
        summary = "Get my notifications",
        description = "Retrieves notifications for the authenticated user (paged)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                notificationService.getMyNotifications(
                        UUID.fromString(userId),
                        unreadOnly,
                        pageable
                )
        );
    }

    @Operation(
        summary = "Get unread notifications count",
        description = "Returns the number of unread notifications for the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        return ResponseEntity.ok(
                notificationService.countUnread(UUID.fromString(userId))
        );
    }

    @Operation(
        summary = "Mark one notification as read",
        description = "Marks the given notification as read for the authenticated user and returns the new unread count."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Long> markOneRead(
            @AuthenticationPrincipal(expression = "username") String userId,
            @PathVariable UUID notificationId
    ) {
        long unreadCount = notificationService.markOneAsRead(
                UUID.fromString(userId),
                notificationId
        );

        return ResponseEntity.ok(unreadCount);
    }

    @Operation(
        summary = "Mark all my notifications as read",
        description = "Marks all unread notifications of the authenticated user as read and returns the new unread count."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications marked as read"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/me/read-all")
    public ResponseEntity<Long> markAllRead(
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        long unreadCount = notificationService.markAllAsRead(UUID.fromString(userId));
        return ResponseEntity.ok(unreadCount);
    }
}
