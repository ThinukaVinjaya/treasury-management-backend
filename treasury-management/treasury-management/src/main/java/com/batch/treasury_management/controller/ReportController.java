package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.UserContributionDetailDTO;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/treasurer/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Monthly Main Batch Fund Report
     * Example: /api/treasurer/reports/main-fund/monthly?monthYear=2026-06
     */
    @GetMapping("/main-fund/monthly")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> generateMonthlyMainFundReport(
            @RequestParam String monthYear) {

        try {
            YearMonth month = YearMonth.parse(monthYear);
            byte[] pdfBytes = reportService.generateMonthlyMainFundReport(month);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "main-fund-report-" + monthYear + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("Error generating report: " + e.getMessage()).getBytes());
        }
    }

    /**
     * All Users / Single User Contribution Status Report (PDF)
     *
     * Examples:
     * - All Users:   ?startMonth=2026-06&endMonth=2026-12
     * - Specific User: ?startMonth=2026-06&endMonth=2026-12&userId=123e4567-e89b-12d3-a456-426614174000
     */
    @GetMapping("/contributions/period")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> generateAllUsersContributionReport(
            @RequestParam String startMonth,
            @RequestParam String endMonth,
            @RequestParam(required = false) String userId) {   // Optional user filter

        try {
            YearMonth from = YearMonth.parse(startMonth);
            YearMonth to = YearMonth.parse(endMonth);

            byte[] pdfBytes = reportService.generateAllUsersContributionReport(from, to, userId);

            String filename = (userId != null && !userId.trim().isEmpty())
                    ? "user-contribution-" + userId.substring(0, 8) + ".pdf"
                    : "contribution-report-" + startMonth + "-to-" + endMonth + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("Error generating report: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Get Detailed Contribution Data for a Specific User (JSON)
     * Useful for frontend tables, dashboards, etc.
     *
     * Example: /api/treasurer/reports/contributions/user/123e4567-e89b-12d3-a456-426614174000
     *          ?startMonth=2026-06&endMonth=2026-12
     */
    @GetMapping("/contributions/user/{userId}")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserContributionDetailDTO>>> getUserContributionDetails(
            @PathVariable String userId,
            @RequestParam String startMonth,
            @RequestParam String endMonth) {

        try {
            YearMonth from = YearMonth.parse(startMonth);
            YearMonth to = YearMonth.parse(endMonth);

            List<UserContributionDetailDTO> data = reportService
                    .getUserContributionDetails(userId, from, to);

            return ResponseEntity.ok(
                    ApiResponse.success("User contribution details fetched successfully", data)
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error fetching user contributions: " + e.getMessage()));
        }
    }
}