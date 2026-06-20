package com.batch.treasury_management.service;

import com.batch.treasury_management.dto.ContributionRequest;
import com.batch.treasury_management.dto.ContributionResponse;
import com.batch.treasury_management.dto.TransactionRequest;
import com.batch.treasury_management.dto.TransactionResponse;
import com.batch.treasury_management.entity.Contribution;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.ContributionRepository;
import com.batch.treasury_management.repository.UserRepository;
import com.batch.treasury_management.security.JwtTokenProvider;
import com.batch.treasury_management.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TransactionService transactionService;

    /**
     * Record Contribution - Enforce Sequential Payment (Previous months must be paid)
     */
    public ContributionResponse recordContribution(ContributionRequest request) {
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        YearMonth requestedMonth = request.getMonth();

        // For Main Fund Contribution only
        if (request.getEventId() == null || request.getEventId().isBlank()) {

            boolean allPreviousPaid = isAllPreviousMonthsPaid(request.getUserId(), requestedMonth);

            if (!allPreviousPaid) {
                throw new IllegalStateException(
                        "User must pay all previous months before paying for " + requestedMonth +
                                ". Please complete previous pending contributions first."
                );
            }

            // Prevent duplicate entry for same month
            boolean alreadyExists = contributionRepository
                    .existsByUserIdAndMonthAndEventIdIsNull(request.getUserId(), requestedMonth);

            if (alreadyExists) {
                throw new IllegalStateException("Contribution for month " + requestedMonth + " already exists for this user");
            }
        }

        Contribution contribution = new Contribution();
        contribution.setUserId(request.getUserId());
        contribution.setMonth(requestedMonth);
        contribution.setAmount(request.getAmount());
        contribution.setEventId(request.getEventId());
        contribution.setPaid(false);

        Contribution saved = contributionRepository.save(contribution);

        auditService.logAction("CREATE_CONTRIBUTION", "CONTRIBUTION", saved.getId(),
                getCurrentUser(), "Amount: " + request.getAmount() + ", Month: " + requestedMonth);

        return mapToResponse(saved);
    }

    /**
     * Check if ALL previous months are paid (Main Fund only)
     */
    private boolean isAllPreviousMonthsPaid(String userId, YearMonth targetMonth) {
        List<Contribution> allMainFundContributions = contributionRepository
                .findByUserIdAndIsDeletedFalse(userId).stream()
                .filter(c -> c.getEventId() == null)
                .collect(Collectors.toList());

        if (allMainFundContributions.isEmpty()) {
            return true;
        }

        YearMonth startMonth = YearMonth.of(2026, 6);

        if (targetMonth.isBefore(startMonth)) {
            return true;
        }

        YearMonth checkMonth = startMonth;
        YearMonth endCheck = targetMonth.minusMonths(1);

        while (!checkMonth.isAfter(endCheck)) {
            final YearMonth currentCheck = checkMonth;

            boolean paidThisMonth = allMainFundContributions.stream()
                    .anyMatch(c -> c.getMonth().equals(currentCheck) && c.isPaid());

            if (!paidThisMonth) {
                return false;
            }

            checkMonth = checkMonth.plusMonths(1);
        }

        return true;
    }

    @Transactional
    public ContributionResponse markContributionAsPaid(String contributionId, String performedBy) {
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (contribution.isPaid()) {
            throw new IllegalStateException("This contribution has already been paid");
        }

        if (contribution.getTransactionId() != null) {
            throw new IllegalStateException("This contribution is already linked to a transaction");
        }

        // Create Income Transaction
        TransactionRequest txRequest = new TransactionRequest();
        txRequest.setTitle("Monthly Contribution - " + contribution.getMonth());
        txRequest.setAmount(contribution.getAmount());
        txRequest.setType("INCOME");
        txRequest.setCategory("CONTRIBUTION");
        txRequest.setDescription("Payment from User: " + contribution.getUserId());
        txRequest.setEventId(contribution.getEventId());

        try {
            TransactionResponse txResponse = transactionService.createTransaction(txRequest, null, performedBy);

            contribution.setPaid(true);
            contribution.setTransactionId(txResponse.getId());

            Contribution saved = contributionRepository.save(contribution);

            auditService.logAction("MARK_CONTRIBUTION_PAID", "CONTRIBUTION", contributionId,
                    performedBy, "Linked to transaction: " + txResponse.getId());

            return mapToResponse(saved);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create payment transaction for contribution", e);
        }
    }

    /**
     * ✅ SOFT DELETE CONTRIBUTION WITH LINKED TRANSACTION HANDLING
     */
    @Transactional
    public void softDeleteContribution(String contributionId, String deletedBy) {
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found with id: " + contributionId));

        if (contribution.isDeleted()) {
            throw new IllegalStateException("Contribution is already deleted");
        }

        contribution.softDelete();
        contributionRepository.save(contribution);

        // If this contribution was paid, also soft delete the linked transaction
        if (contribution.isPaid() && contribution.getTransactionId() != null) {
            try {
                transactionService.softDeleteTransaction(contribution.getTransactionId(), deletedBy);
            } catch (Exception e) {
                System.err.println("Warning: Failed to delete linked transaction: " + e.getMessage());
            }
        }

        auditService.logAction("SOFT_DELETE_CONTRIBUTION", "CONTRIBUTION", contributionId, deletedBy,
                "Month: " + contribution.getMonth() + " | Amount: " + contribution.getAmount());

        System.out.println("✅ Contribution soft deleted successfully.");
    }

    // ==================== OTHER METHODS ====================

    public ContributionResponse getContributionById(String id) {
        Contribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));
        return mapToResponse(contribution);
    }

    public ContributionResponse updateContribution(String id, ContributionRequest request) {
        Contribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        contribution.setMonth(request.getMonth());
        contribution.setAmount(request.getAmount());
        if (request.getEventId() != null) {
            contribution.setEventId(request.getEventId());
        }

        Contribution updated = contributionRepository.save(contribution);

        auditService.logAction("UPDATE_CONTRIBUTION", "CONTRIBUTION", id,
                getCurrentUser(), "Updated amount: " + request.getAmount());

        return mapToResponse(updated);
    }

    public Page<ContributionResponse> getUserContributionsPaginated(String userId, Pageable pageable) {
        return contributionRepository.findByUserIdAndIsDeletedFalse(userId, pageable)
                .map(this::mapToResponse);
    }

    public Page<ContributionResponse> getEventContributionsPaginated(String eventId, Pageable pageable) {
        return contributionRepository.findByEventIdAndIsDeletedFalse(eventId, pageable)
                .map(this::mapToResponse);
    }

    public Page<ContributionResponse> getUserEventContributions(String userId, String eventId, Pageable pageable) {
        return contributionRepository
                .findByUserIdAndEventIdAndIsDeletedFalse(userId, eventId, pageable)
                .map(this::mapToResponse);
    }

    public List<ContributionResponse> getPaidContributionsByMonth(YearMonth month) {
        List<Contribution> paid = contributionRepository
                .findByEventIdIsNullAndMonthAndIsPaidTrueAndIsDeletedFalse(month);

        return paid.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ContributionResponse mapToResponse(Contribution c) {
        ContributionResponse response = new ContributionResponse();
        response.setId(c.getId());
        response.setUserId(c.getUserId());
        response.setMonth(c.getMonth());
        response.setAmount(c.getAmount());
        response.setPaid(c.isPaid());
        response.setEventId(c.getEventId());
        response.setCreatedAt(c.getCreatedAt());
        return response;
    }

    private String getCurrentUser() {
        return jwtTokenProvider.getCurrentUsername() != null ? jwtTokenProvider.getCurrentUsername() : "SYSTEM";
    }
}