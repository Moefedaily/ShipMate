package com.shipmate.mapper.notification;

import com.shipmate.dto.response.notification.NotificationResponse;
import com.shipmate.model.notification.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
