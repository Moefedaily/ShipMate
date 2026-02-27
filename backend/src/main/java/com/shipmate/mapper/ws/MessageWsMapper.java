package com.shipmate.mapper.ws;

import com.shipmate.dto.ws.message.MessageWsDto;
import com.shipmate.model.message.Message;

public class MessageWsMapper {

    public static MessageWsDto toWsDto(Message message) {
        return new MessageWsDto(
                message.getId(),
                message.getShipment().getId(),
                message.getMessageType(),
                message.getMessageContent(),
                message.getSentAt()
        );
    }
}
