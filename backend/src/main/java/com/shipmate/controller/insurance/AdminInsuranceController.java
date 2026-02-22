package com.shipmate.controller.insurance;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.dto.response.insurance.InsuranceClaimResponse;
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
    public ResponseEntity<List<InsuranceClaimResponse>> listClaims(
            @RequestParam(required = false) ClaimStatus status
    ) {
        return ResponseEntity.ok(claimService.listClaims(status));
    }


    @GetMapping("/claims/{id}")
    @Operation(summary = "Get claim by ID", description = "Retrieve a specific claim by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<InsuranceClaimResponse> getClaimById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(claimService.getById(id));
    }
}    

