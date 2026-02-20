package com.shipmate.mapper.ws;

import com.shipmate.dto.ws.notification.NotificationWsDto;
import com.shipmate.model.notification.Notification;

public class NotificationWsMapper {

    public static NotificationWsDto toWsDto(Notification notification) {
        return new NotificationWsDto(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getNotificationType(),
                notification.getReferenceId(),
                notification.getReferenceType(),
                notification.getCreatedAt()
        );
    }

}
