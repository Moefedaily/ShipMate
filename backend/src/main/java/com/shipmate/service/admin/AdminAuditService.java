package com.shipmate.service.admin;

import com.shipmate.dto.response.admin.AdminAuditResponse;
import com.shipmate.model.admin.*;
import com.shipmate.repository.admin.AdminActionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminActionRepository repository;

    public void log(
            UUID adminId,
            AdminTargetType targetType,
            UUID targetId,
            AdminActionType action,
            String note
    ) {

        AdminAction entry = AdminAction.builder()
                .id(UUID.randomUUID())
                .adminId(adminId)
                .targetType(targetType)
                .targetId(targetId)
                .action(action)
                .note(note)
                .createdAt(Instant.now())
                .build();

        repository.save(entry);
    }
 @Transactional(readOnly = true)
    public Page<AdminAuditResponse> getLogs(Pageable pageable) {

        return repository
                .findAll(pageable)
                .map(audit -> new AdminAuditResponse(
                        audit.getId(),
                        audit.getAdminId(),
                        audit.getTargetType(),
                        audit.getTargetId(),
                        audit.getAction(),
                        audit.getNote(),
                        audit.getCreatedAt()
                ));
    }   

    @Transactional(readOnly = true)
    public Page<AdminAuditResponse> getLogsForTarget(
            AdminTargetType targetType,
            UUID targetId,
            Pageable pageable
    ) {

        return repository
                .findByTargetTypeAndTargetId(targetType, targetId, pageable)
                .map(audit -> new AdminAuditResponse(
                        audit.getId(),
                        audit.getAdminId(),
                        audit.getTargetType(),
                        audit.getTargetId(),
                        audit.getAction(),
                        audit.getNote(),
                        audit.getCreatedAt()
                ));
    }
}