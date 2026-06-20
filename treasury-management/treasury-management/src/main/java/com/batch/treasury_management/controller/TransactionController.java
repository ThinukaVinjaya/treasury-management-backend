package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.TransactionRequest;
import com.batch.treasury_management.dto.TransactionResponse;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Create Income / Expense Transaction with Optional Proof
     * - Proof is mandatory for EXPENSE transactions
     */
    @PostMapping("/treasurer/transactions")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @ModelAttribute TransactionRequest request,
            @RequestParam(required = false) MultipartFile file,
            Authentication authentication) throws IOException {

        String uploadedBy = authentication.getName();

        // Mandatory receipt for EXPENSE
        if ("EXPENSE".equalsIgnoreCase(request.getType()) && (file == null || file.isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Proof/receipt file is mandatory for all EXPENSE transactions"));
        }

        TransactionResponse response = transactionService.createTransaction(request, file, uploadedBy);

        String message = (file != null && !file.isEmpty())
                ? "Transaction created successfully with proof uploaded"
                : "Transaction created successfully";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /**
     * View Receipt / Proof (Redirect to Cloudinary)
     */
    @GetMapping("/files/{transactionId}")
    public ResponseEntity<?> viewFile(@PathVariable String transactionId) {
        TransactionResponse transaction = transactionService.getTransactionById(transactionId);

        if (transaction.getFileUrl() == null || transaction.getFileUrl().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No receipt attached to this transaction"));
        }

        // Add cache buster to avoid browser caching issues
        String publicUrl = transaction.getFileUrl() + "?_=" + System.currentTimeMillis();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, publicUrl)
                .build();
    }

    /**
     * Get Main Fund Transactions (Paginated)
     */
    @GetMapping("/treasurer/transactions/main")
    @PreAuthorize("hasAnyRole('USER', 'TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getMainFundTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = transactionService.getMainFundTransactionsPaginated(pageable);

        return ResponseEntity.ok(ApiResponse.success("Main fund transactions fetched successfully", transactions));
    }

    /**
     * Get Event Transactions (Paginated)
     */
    @GetMapping("/treasurer/transactions/event/{eventId}")
    @PreAuthorize("hasAnyRole('USER', 'TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getEventTransactions(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = transactionService.getEventTransactionsPaginated(eventId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Event transactions fetched successfully", transactions));
    }

    /**
     * ✅ SOFT DELETE TRANSACTION
     * - Updates balance automatically
     * - Logs audit
     */
    @DeleteMapping("/treasurer/transactions/{id}")
    @PreAuthorize("hasAnyRole('TREASURER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> softDeleteTransaction(
            @PathVariable String id,
            Authentication authentication) {

        transactionService.softDeleteTransaction(id, authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success("Transaction soft deleted successfully. Balance has been updated.")
        );
    }
}