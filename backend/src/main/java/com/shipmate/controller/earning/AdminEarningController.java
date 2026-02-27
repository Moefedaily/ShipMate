package com.shipmate.controller.earning;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.service.earning.DriverEarningService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/earnings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEarningController {

    private final DriverEarningService driverEarningService;

    @PatchMapping("/{earningId}/mark-paid")
    public ResponseEntity<Void> markAsPaid(@PathVariable UUID earningId) {
        driverEarningService.markAsPaid(earningId);
        return ResponseEntity.noContent().build();
    }

}