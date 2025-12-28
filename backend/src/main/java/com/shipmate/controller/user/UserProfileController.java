package com.shipmate.controller.user;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.shipmate.dto.request.user.UpdateUserProfileRequest;
import com.shipmate.dto.response.user.UserProfileResponse;
import com.shipmate.service.user.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management APIs")
public class UserProfileController {
    
    private final UserProfileService userProfileService;

    // ===================== GET MY PROFILE =====================
    @Operation(
        summary = "Get my profile",
        description = "Retrieves the authenticated user's profile information."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal(expression = "username") String userId) {
        return ResponseEntity.ok(userProfileService.getMyProfile(UUID.fromString(userId)));
    }

    // ===================== UPDATE MY PROFILE =====================
    @Operation(
        summary = "Update my profile",
        description = "Updates the authenticated user's profile information."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateMyProfile(UUID.fromString(userId), request));
    }
}