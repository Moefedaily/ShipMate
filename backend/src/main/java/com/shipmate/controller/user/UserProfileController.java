package com.shipmate.controller.user;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.shipmate.dto.request.user.UpdateUserProfileRequest;
import com.shipmate.dto.response.user.UserProfileResponse;
import com.shipmate.service.user.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateMyProfile(UUID.fromString(userId), request));
    }

    // ===================== UPLOAD AVATAR =====================
    @Operation(
        summary = "Upload user avatar",
        description = "Uploads a new avatar image for the authenticated user. Supported formats: JPG, PNG, GIF. Maximum file size: 5MB."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Avatar uploaded successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserProfileResponse.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid file format or file too large"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(
        value = "/me/avatar", 
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestPart("file") 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Avatar image file",
                required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            MultipartFile file) {
        return ResponseEntity.ok(
            userProfileService.updateAvatar(UUID.fromString(userId), file)
        );
    }

    // ===================== DELETE AVATAR =====================
    @Operation(
        summary = "Delete user avatar",
        description = "Removes the authenticated user's avatar image and reverts to the default avatar."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Avatar deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Avatar not found")
    })
    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal(expression = "username") String userId) {
        userProfileService.deleteAvatar(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}