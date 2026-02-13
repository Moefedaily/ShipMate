package com.shipmate.dto.ws.typing;


import java.util.UUID;

public record TypingWsDto(
        UUID userId,
        String displayName
) {}
