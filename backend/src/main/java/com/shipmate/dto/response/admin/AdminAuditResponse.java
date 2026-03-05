package com.shipmate.dto.response.admin;

import java.time.Instant;
import java.util.UUID;

import com.shipmate.model.admin.AdminActionType;
import com.shipmate.model.admin.AdminTargetType;

public record AdminAuditResponse(

        UUID id,
        UUID adminId,

        AdminTargetType targetType,
        UUID targetId,

        AdminActionType actionType,

        String note,

        Instant createdAt
) {}