package com.shipmate.controller.admin;

import com.shipmate.dto.response.admin.AdminDashboardResponse;
import com.shipmate.service.admin.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard", description = "Admin platform overview metrics")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(
        summary = "Get admin dashboard metrics",
        description = "Returns aggregated platform statistics such as users, drivers, shipments, claims and revenue."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> getDashboard() {

        AdminDashboardResponse response =
                adminDashboardService.getDashboardMetrics();

        return ResponseEntity.ok(response);
    }
}