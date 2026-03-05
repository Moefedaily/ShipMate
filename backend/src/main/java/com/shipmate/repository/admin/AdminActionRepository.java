package com.shipmate.repository.admin;

import com.shipmate.model.admin.AdminAction;
import com.shipmate.model.admin.AdminTargetType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {
    Page<AdminAction> findByTargetTypeAndTargetId(
        AdminTargetType targetType,
        UUID targetId,
        Pageable pageable
);
}