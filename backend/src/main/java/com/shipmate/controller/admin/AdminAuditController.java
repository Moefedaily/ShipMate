package com.shipmate.controller.admin;

import com.shipmate.dto.response.admin.AdminAuditResponse;
import com.shipmate.model.admin.AdminTargetType;
import com.shipmate.service.admin.AdminAuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AdminAuditService auditService;

    @GetMapping
    @Operation(summary = "List audit logs", description = "List all admin action logs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs listed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Page<AdminAuditResponse>> getAuditLogs(
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                auditService.getLogs(pageable)
        );
    }

    @GetMapping("/{targetType}/{targetId}")
    @Operation(summary = "Get audit logs for target", description = "List admin action logs for a specific target")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs listed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Target not found")
    })
    public ResponseEntity<Page<AdminAuditResponse>> getTargetHistory(
            @PathVariable AdminTargetType targetType,
            @PathVariable UUID targetId,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                auditService.getLogsForTarget(targetType, targetId, pageable)
        );
    }
}