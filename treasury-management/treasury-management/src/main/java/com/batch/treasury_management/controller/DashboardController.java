package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.DashboardSummary;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard data fetched",
                dashboardService.getDashboardSummary()));
    }
}