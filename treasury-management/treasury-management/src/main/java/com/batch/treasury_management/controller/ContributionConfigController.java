package com.batch.treasury_management.controller;

import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.ContributionConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/treasurer/contribution-config")
@RequiredArgsConstructor
public class ContributionConfigController {

    private final ContributionConfigService contributionConfigService;

    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> getDefaultMonthlyAmount() {
        BigDecimal amount = contributionConfigService.getDefaultMonthlyContribution();
        return ResponseEntity.ok(ApiResponse.success("Default monthly contribution fetched", amount));
    }

    @PutMapping("/default")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateDefaultMonthlyAmount(@RequestParam BigDecimal amount) {
        contributionConfigService.updateDefaultMonthlyContribution(amount);
        return ResponseEntity.ok(ApiResponse.success("Default monthly contribution updated successfully to: " + amount));
    }
}