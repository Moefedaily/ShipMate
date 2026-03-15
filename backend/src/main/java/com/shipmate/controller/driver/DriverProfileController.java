package com.shipmate.controller.driver;

import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.dto.request.driver.UpdateLicenseRequest;
import com.shipmate.dto.request.driver.UpdateDriverLocationRequest;
import com.shipmate.dto.response.driver.DriverProfileResponse;
import com.shipmate.service.driver.DriverProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Profile", description = "Driver profile management APIs")
public class DriverProfileController {

    private final DriverProfileService driverProfileService;

    // ===================== APPLY AS DRIVER =====================

   @Operation(
        summary = "Apply to become a driver",
        description = "Creates a pending driver profile for the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
        @ApiResponse(responseCode = "400", description = "User already has a driver profile"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/apply")
    public ResponseEntity<DriverProfileResponse> apply(
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody DriverApplyRequest request
    ) {
        DriverProfileResponse response =
                driverProfileService.apply(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DriverProfileResponse> applyWithLicensePhotos(
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestPart("request") @Valid DriverApplyRequest request,
            @RequestPart("files") List<MultipartFile> files
    ) {
        DriverProfileResponse response =
                driverProfileService.applyWithLicensePhotos(UUID.fromString(userId), request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ===================== GET MY PROFILE =====================

    @Operation(
        summary = "Get my driver profile",
        description = "Retrieves the authenticated user's driver profile."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Driver profile not found")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<DriverProfileResponse> getMyProfile(
            @AuthenticationPrincipal(expression = "username") String userId
    ) {
        return ResponseEntity.ok(
                driverProfileService.getMyProfile(UUID.fromString(userId))
        );
    }

    @PutMapping("/me/license")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DriverProfileResponse> updateLicense(
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody UpdateLicenseRequest request
    ) {
        return ResponseEntity.ok(
                driverProfileService.updateLicense(UUID.fromString(userId), request)
        );
    }

    @PostMapping(value = "/me/license/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DriverProfileResponse> uploadLicensePhotos(
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(
                driverProfileService.uploadLicensePhotos(UUID.fromString(userId), files)
        );
    }

    // ===================== UPDATE MY LOCATION =====================

    @Operation(
        summary = "Update my location",
        description = "Updates the authenticated user's location."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Location updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("@driverSecurity.isApprovedDriver(authentication)")
    @PostMapping("/me/location")
    public void updateMyLocation(
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestBody UpdateDriverLocationRequest request
    ) {
             driverProfileService.updateLocation(UUID.fromString(userId), request);
        }

}
