package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.ContributionRequest;
import com.batch.treasury_management.dto.ContributionResponse;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.UserRepository;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.ContributionReminderService;
import com.batch.treasury_management.service.ContributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;
    private final ContributionReminderService contributionReminderService;
    private final UserRepository userRepository;

    /**
     * Get My Contributions for a Specific Event (For Users / Temp Treasurers)
     */
    @GetMapping("/user/events/{eventId}/contributions")
    public ResponseEntity<ApiResponse<Page<ContributionResponse>>> getMyEventContributions(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ContributionResponse> contributions =
                contributionService.getUserEventContributions(user.getId(), eventId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Event contributions fetched successfully", contributions));
    }

    /**
     * Record New Contribution
     */
    @PostMapping("/treasurer/contributions")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ContributionResponse>> recordContribution(
            @RequestBody ContributionRequest request) {

        ContributionResponse response = contributionService.recordContribution(request);

        return ResponseEntity.ok(
                ApiResponse.success("Contribution recorded successfully", response));
    }

    /**
     * Get Contribution by ID
     */
    @GetMapping("/treasurer/contributions/{id}")
    @PreAuthorize("hasAnyRole('USER', 'TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ContributionResponse>> getContributionById(
            @PathVariable String id) {

        ContributionResponse response = contributionService.getContributionById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Contribution fetched successfully", response));
    }

    /**
     * Get Logged-in User's Contributions (or any user's if admin/treasurer)
     */
    @GetMapping("/user/contributions")
    public ResponseEntity<ApiResponse<Page<ContributionResponse>>> getMyContributions(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String actualUserId = userId;

        if (actualUserId == null || actualUserId.isBlank()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            actualUserId = user.getId();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ContributionResponse> contributions =
                contributionService.getUserContributionsPaginated(actualUserId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Contributions fetched successfully", contributions));
    }

    /**
     * Get All Contributions for an Event
     */
    @GetMapping("/treasurer/events/{eventId}/contributions")
    @PreAuthorize("hasAnyRole('USER', 'TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<ContributionResponse>>> getEventContributions(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ContributionResponse> contributions =
                contributionService.getEventContributionsPaginated(eventId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Event contributions fetched successfully", contributions));
    }

    /**
     * Mark Contribution as Paid (Creates Income Transaction)
     */
    @PutMapping("/treasurer/contributions/{id}/pay")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ContributionResponse>> markAsPaid(
            @PathVariable String id,
            Authentication authentication) {

        String performedBy = authentication.getName();

        ContributionResponse response = contributionService.markContributionAsPaid(id, performedBy);

        return ResponseEntity.ok(
                ApiResponse.success("Contribution marked as paid successfully. Income transaction created.", response));
    }

    /**
     * Update Contribution
     */
    @PutMapping("/treasurer/contributions/{id}")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ContributionResponse>> updateContribution(
            @PathVariable String id,
            @RequestBody ContributionRequest request) {

        ContributionResponse response = contributionService.updateContribution(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Contribution updated successfully", response));
    }

    /**
     * ✅ SOFT DELETE CONTRIBUTION
     * - Also deletes linked transaction if paid
     * - Updates balances automatically
     */
    @DeleteMapping("/treasurer/contributions/{id}")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> softDeleteContribution(
            @PathVariable String id,
            Authentication authentication) {

        String deletedBy = authentication.getName();

        contributionService.softDeleteContribution(id, deletedBy);

        return ResponseEntity.ok(
                ApiResponse.success("Contribution soft deleted successfully. Related balances updated."));
    }

    /**
     * Send Monthly Contribution Reminders
     */
    @PostMapping("/treasurer/reminders/send")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> sendReminders() {

        contributionReminderService.sendMonthlyReminders();

        return ResponseEntity.ok(
                ApiResponse.success("Monthly contribution reminders sent successfully"));
    }
}