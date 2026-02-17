package com.shipmate.controller.earning;

import com.shipmate.dto.response.earning.DriverEarningResponse;
import com.shipmate.dto.response.earning.DriverEarningsSummaryResponse;
import com.shipmate.service.earning.DriverEarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/driver/earnings")
@RequiredArgsConstructor
@Tag(name = "Driver Earnings", description = "Driver earnings management APIs")
public class DriverEarningController {
    
    private final DriverEarningService driverEarningService;

    @Operation(
        summary = "Get driver earnings",
        description = "Retrieve paginated earnings history for the authenticated driver. Includes gross amount, commission, net amount, and payout status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Earnings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - User is not a driver")
    })
    @GetMapping
    public ResponseEntity<Page<DriverEarningResponse>> getMyEarnings(
            @AuthenticationPrincipal(expression = "username") String userId,
            Pageable pageable
    ) {
        Page<DriverEarningResponse> response =
                driverEarningService.getMyEarnings(
                        UUID.fromString(userId),
                        pageable
                );
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Get driver earnings summary",
            description = "Retrieve earnings summary for the authenticated driver. Includes total gross amount, total commission, total net amount, and total payout status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Earnings summary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not a driver")
    })
    @GetMapping("/summary")
    public ResponseEntity<DriverEarningsSummaryResponse> getMyEarningsSummary(
            @AuthenticationPrincipal(expression = "username") String userId
    ) {

        DriverEarningsSummaryResponse response =
                driverEarningService.getMyEarningsSummary(
                        UUID.fromString(userId)
                );

        return ResponseEntity.ok(response);
    }

}