package com.shipmate.controller.insurance;

import com.shipmate.dto.request.insurance.AdminClaimDecisionRequest;
import com.shipmate.dto.request.insurance.CreateInsuranceClaimRequest;
import com.shipmate.dto.response.insurance.InsuranceClaimResponse;
import com.shipmate.service.insurance.InsuranceClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/insurance")
@RequiredArgsConstructor
@Tag(name = "Insurance Claims", description = "Insurance claim management APIs")
public class InsuranceClaimController {

    private final InsuranceClaimService claimService;

    @Operation(
            summary = "Submit insurance claim",
            description = "Submit an insurance claim for a shipment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid claim request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/shipments/{shipmentId}")
    public ResponseEntity<InsuranceClaimResponse> submitClaim(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestBody CreateInsuranceClaimRequest request
    ) {

        InsuranceClaimResponse response =
                claimService.submitClaim(
                        shipmentId,
                        UUID.fromString(userId),
                        request
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get insurance claim for shipment",
            description = "Retrieve insurance claim details for a shipment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @GetMapping("/shipments/{shipmentId}")
    public ResponseEntity<InsuranceClaimResponse> getClaimByShipment(
            @PathVariable UUID shipmentId
    ) {

        InsuranceClaimResponse response =
                claimService.getByShipment(shipmentId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Review insurance claim (Admin)",
            description = "Approve or reject an insurance claim."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim reviewed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid decision"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{claimId}/review")
    public ResponseEntity<InsuranceClaimResponse> reviewClaim(
            @PathVariable UUID claimId,
            @AuthenticationPrincipal(expression = "username") String adminUserId,
            @RequestBody AdminClaimDecisionRequest request
    ) {

        InsuranceClaimResponse response =
                claimService.reviewClaim(
                        claimId,
                        UUID.fromString(adminUserId),
                        request
                );

        return ResponseEntity.ok(response);
    }
    @PostMapping(
        value = "/claims/{claimId}/photos",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        )
        public ResponseEntity<InsuranceClaimResponse> addClaimPhotos(
                @PathVariable UUID claimId,
                @AuthenticationPrincipal(expression = "username") String userId,
                @RequestPart("files") List<MultipartFile> files
        ) {
        return ResponseEntity.ok(
                claimService.addClaimPhotos(
                        claimId,
                        UUID.fromString(userId),
                        files
                )
        );
        }

        @GetMapping("/me")
        public ResponseEntity<List<InsuranceClaimResponse>> getMyClaims(
                @AuthenticationPrincipal(expression = "username") String userId) {

        return ResponseEntity.ok(
                claimService.listMyClaims(UUID.fromString(userId))
        );
        }
}