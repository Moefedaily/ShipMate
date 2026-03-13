package com.shipmate.controller.insurance;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.dto.request.insurance.AdminClaimDecisionRequest;
import com.shipmate.dto.request.insurance.AdminClaimNotesRequest;
import com.shipmate.dto.response.admin.AdminClaimResponse;
import com.shipmate.model.insuranceClaim.ClaimStatus;
import com.shipmate.service.insurance.InsuranceClaimService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/insurance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Insurance Claims", description = "Admin APIs for insurance claim management")
public class AdminInsuranceController {

    private final InsuranceClaimService claimService;

    @GetMapping("/claims")
    @Operation(summary = "List claims", description = "List all claims")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claims listed successfully")
    })
	public ResponseEntity<Page<AdminClaimResponse>> listClaims(
			@RequestParam(required = false) ClaimStatus status,
            Pageable pageable
	) {
		return ResponseEntity.ok(claimService.listAdminClaims(status, pageable));
	}


    @GetMapping("/claims/{id}")
    @Operation(summary = "Get claim by ID", description = "Retrieve a specific claim by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<AdminClaimResponse> getClaimById(
			@PathVariable UUID id
	) {
		return ResponseEntity.ok(claimService.getAdminClaim(id));
	}

      @Operation(
            summary = "Review insurance claim",
            description = "Approve or reject an insurance claim."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim reviewed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid decision"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{claimId}/review")
    public ResponseEntity<AdminClaimResponse> reviewClaim(
        @PathVariable UUID claimId,
        @AuthenticationPrincipal(expression = "username") String adminUserId,
        @RequestBody AdminClaimDecisionRequest request
	) {
		return ResponseEntity.ok(
				claimService.reviewAdminClaim(
						claimId,
						UUID.fromString(adminUserId),
						request
				)
		);
	}
	@PostMapping("/claims/{claimId}/approve")
	@Operation(summary = "Approve claim", description = "Approve an insurance claim")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Claim approved successfully"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
		@ApiResponse(responseCode = "404", description = "Claim not found")
	})
    public ResponseEntity<AdminClaimResponse> approve(
            @PathVariable UUID claimId,
            @AuthenticationPrincipal(expression = "username") String adminUserId,
            @RequestBody(required = false) AdminClaimNotesRequest body
    ) {
        AdminClaimDecisionRequest req = new AdminClaimDecisionRequest();
        req.setDecision(ClaimStatus.APPROVED);
        req.setAdminNotes(body != null ? body.getAdminNotes() : null);

        return ResponseEntity.ok(
                claimService.reviewAdminClaim(
                        claimId,
                        UUID.fromString(adminUserId),
                        req
                )
        );
    }

    @PostMapping("/claims/{claimId}/reject")
	@Operation(summary = "Reject claim", description = "Reject an insurance claim")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Claim rejected successfully"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
		@ApiResponse(responseCode = "404", description = "Claim not found")
	})
    public ResponseEntity<AdminClaimResponse> reject(
            @PathVariable UUID claimId,
            @AuthenticationPrincipal(expression = "username") String adminUserId,
            @RequestBody(required = false) AdminClaimNotesRequest body
    ) {
        AdminClaimDecisionRequest req = new AdminClaimDecisionRequest();
        req.setDecision(ClaimStatus.REJECTED);
        req.setAdminNotes(body != null ? body.getAdminNotes() : null);

        return ResponseEntity.ok(
                claimService.reviewAdminClaim(
                        claimId,
                        UUID.fromString(adminUserId),
                        req
                )
        );
    }
}
