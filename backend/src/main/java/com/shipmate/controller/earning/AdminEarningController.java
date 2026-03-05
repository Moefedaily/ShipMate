package com.shipmate.controller.earning;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.dto.response.earning.DriverEarningResponse;
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

    @GetMapping
    public ResponseEntity<Page<DriverEarningResponse>> getEarnings(Pageable pageable) {
        return ResponseEntity.ok(driverEarningService.getAllEarnings(pageable));
    }
}