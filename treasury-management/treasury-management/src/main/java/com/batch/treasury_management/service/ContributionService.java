package com.batch.treasury_management.service;

import com.batch.treasury_management.dto.ContributionRequest;
import com.batch.treasury_management.dto.ContributionResponse;
import com.batch.treasury_management.dto.TransactionRequest;
import com.batch.treasury_management.dto.TransactionResponse;
import com.batch.treasury_management.entity.Contribution;
import com.batch.treasury_management.entity.Event;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.ContributionRepository;
import com.batch.treasury_management.repository.EventRepository;
import com.batch.treasury_management.repository.UserRepository;
import com.batch.treasury_management.security.JwtTokenProvider;
import com.batch.treasury_management.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    private final EventRepository eventRepository;
    private final AuditService auditService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TransactionService transactionService;

    @Transactional
    public ContributionResponse recordContribution(ContributionRequest request, String performedBy) {
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (request.getEventId() != null && !request.getEventId().trim().isEmpty()) {
            validateEventTreasurerPermission(request.getEventId(), performedBy);
        }

        YearMonth requestedMonth = request.getMonth();

        if (request.getEventId() == null || request.getEventId().trim().isEmpty()) {
            boolean allPreviousPaid = isAllPreviousMonthsPaid(request.getUserId(), requestedMonth);

            if (!allPreviousPaid) {
                auditService.logAction("CREATE_CONTRIBUTION_WARNING", "CONTRIBUTION", null,
                        performedBy, "Previous months not fully paid");
            }

            boolean alreadyExists = contributionRepository
                    .existsByUserIdAndMonthAndEventIdIsNull(request.getUserId(), requestedMonth);

            if (alreadyExists) {
                throw new IllegalStateException("Contribution for this month already exists");
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
                performedBy, "Amount: " + request.getAmount() + ", Month: " + requestedMonth);

        return mapToResponse(saved);
    }

    private void validateEventTreasurerPermission(String eventId, String username) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("SUPER_ADMIN".equals(currentUser.getRole())) return;

        boolean isMain = currentUser.getId().equals(event.getTreasurerId());
        boolean isTemp = event.getTemporaryTreasurerId() != null &&
                currentUser.getId().equals(event.getTemporaryTreasurerId());

        if (!isMain && !isTemp) {
            throw new AccessDeniedException("You are not authorized for this event.");
        }
    }

    private boolean isAllPreviousMonthsPaid(String userId, YearMonth targetMonth) {
        List<Contribution> allMainFund = contributionRepository
                .findByUserIdAndIsDeletedFalse(userId).stream()
                .filter(c -> c.getEventId() == null)
                .collect(Collectors.toList());

        if (allMainFund.isEmpty()) return true;

        YearMonth startMonth = YearMonth.of(2026, 6);
        if (targetMonth.isBefore(startMonth)) return true;

        YearMonth checkMonth = startMonth;
        YearMonth endCheck = targetMonth.minusMonths(1);

        while (!checkMonth.isAfter(endCheck)) {
            final YearMonth current = checkMonth;
            boolean paid = allMainFund.stream()
                    .anyMatch(c -> c.getMonth().equals(current) && c.isPaid());
            if (!paid) return false;
            checkMonth = checkMonth.plusMonths(1);
        }
        return true;
    }

    @Transactional
    public ContributionResponse markContributionAsPaid(String contributionId, String performedBy) throws IOException {
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (contribution.isPaid()) throw new IllegalStateException("Already paid");
        if (contribution.getTransactionId() != null) throw new IllegalStateException("Already linked");

        TransactionRequest txRequest = new TransactionRequest();
        txRequest.setTitle("Contribution - " + contribution.getMonth());
        txRequest.setAmount(contribution.getAmount());
        txRequest.setType("INCOME");
        txRequest.setCategory("CONTRIBUTION");
        txRequest.setDescription("Payment from user");
        txRequest.setEventId(contribution.getEventId());

        TransactionResponse txResponse = transactionService.createTransaction(txRequest, null, performedBy);

        contribution.setPaid(true);
        contribution.setTransactionId(txResponse.getId());

        Contribution saved = contributionRepository.save(contribution);

        auditService.logAction("MARK_CONTRIBUTION_PAID", "CONTRIBUTION", contributionId,
                performedBy, "Linked to TX: " + txResponse.getId());

        return mapToResponse(saved);
    }

    public ContributionResponse getContributionById(String id) {
        Contribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));
        return mapToResponse(contribution);
    }

    @Transactional
    public ContributionResponse updateContribution(String id, ContributionRequest request) {
        Contribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        contribution.setMonth(request.getMonth());
        contribution.setAmount(request.getAmount());
        if (request.getEventId() != null) contribution.setEventId(request.getEventId());

        Contribution updated = contributionRepository.save(contribution);
        return mapToResponse(updated);
    }

    @Transactional
    public void softDeleteContribution(String contributionId, String deletedBy) {
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        contribution.softDelete();
        contributionRepository.save(contribution);

        if (contribution.isPaid() && contribution.getTransactionId() != null) {
            try {
                transactionService.softDeleteTransaction(contribution.getTransactionId(), deletedBy);
            } catch (Exception e) {
                System.err.println("Warning: Failed to delete linked transaction");
            }
        }

        auditService.logAction("SOFT_DELETE_CONTRIBUTION", "CONTRIBUTION", contributionId, deletedBy,
                "Month: " + contribution.getMonth());
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
        return contributionRepository.findByUserIdAndEventIdAndIsDeletedFalse(userId, eventId, pageable)
                .map(this::mapToResponse);
    }

    public List<ContributionResponse> getPaidContributionsByMonth(YearMonth month) {
        List<Contribution> paid = contributionRepository
                .findByEventIdIsNullAndMonthAndIsPaidTrueAndIsDeletedFalse(month);
        return paid.stream().map(this::mapToResponse).collect(Collectors.toList());
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
        response.setTransactionId(c.getTransactionId());
        return response;
    }
}