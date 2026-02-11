package com.shipmate.mapper.message;

import com.shipmate.dto.response.message.MessageResponse;
import com.shipmate.model.message.Message;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "receiver.id", target = "receiverId")
    MessageResponse toResponse(Message message);
}
