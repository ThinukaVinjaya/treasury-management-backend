package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.*;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.UserRepository;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserViewController {

    private final TransactionService transactionService;
    private final ContributionService contributionService;
    private final EventService eventService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(ApiResponse.success("Current user profile", mapToUserResponse(user)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardSummary>> getMyDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard fetched",
                dashboardService.getDashboardSummary()));
    }

    // Paginated Main Fund Transactions
    @GetMapping("/transactions/main")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getMainFundTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = transactionService.getMainFundTransactionsPaginated(pageable);

        return ResponseEntity.ok(ApiResponse.success("Main fund transactions fetched", transactions));
    }

    // Paginated Event Transactions
    @GetMapping("/transactions/event/{eventId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getEventTransactions(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = transactionService.getEventTransactionsPaginated(eventId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Event transactions fetched", transactions));
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String username = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = transactionService.getTransactionsByUploadedByPaginated(username, pageable);

        return ResponseEntity.ok(ApiResponse.success("My transactions fetched", transactions));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EventResponse> events = eventService.getAllEventsPaginated(pageable);

        return ResponseEntity.ok(ApiResponse.success("All events fetched", events));
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setFirstLogin(user.isFirstLogin());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}