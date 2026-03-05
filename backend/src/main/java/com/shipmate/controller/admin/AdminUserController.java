package com.shipmate.controller.admin;

import com.shipmate.dto.response.admin.AdminUserResponse;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.UserType;
import com.shipmate.service.admin.AdminUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Admin user management APIs")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List users", description = "List all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users listed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {

        Page<AdminUserResponse> response =
                adminUserService.getUsers(
                        role,
                        userType,
                        active,
                        search,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user details", description = "Get detailed information about a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AdminUserResponse> getUser(
            @PathVariable UUID userId
    ) {

        return ResponseEntity.ok(
                adminUserService.getUser(userId)
        );
    }


    @PatchMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate a user account, preventing them from logging in")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deactivateUser(
            @PathVariable UUID userId
    ) {

        adminUserService.deactivateUser(userId);

        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{userId}/activate")
    @Operation(summary = "Activate user", description = "Activate a previously deactivated user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User activated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> activateUser(
            @PathVariable UUID userId
    ) {
        adminUserService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }
}