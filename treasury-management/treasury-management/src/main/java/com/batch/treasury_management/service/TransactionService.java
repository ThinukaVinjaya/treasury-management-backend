package com.batch.treasury_management.service;

import com.batch.treasury_management.dto.TransactionRequest;
import com.batch.treasury_management.dto.TransactionResponse;
import com.batch.treasury_management.entity.Event;
import com.batch.treasury_management.entity.Transaction;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.EventRepository;
import com.batch.treasury_management.repository.TransactionRepository;
import com.batch.treasury_management.repository.UserRepository;
import com.batch.treasury_management.service.AuditService;
import com.batch.treasury_management.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final EventRepository eventRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request,
                                                 MultipartFile file,
                                                 String uploadedBy) throws IOException {

        // Temporary Treasurer Permission Check - FIXED
        if (request.getEventId() != null && !request.getEventId().isBlank()) {
            validateTemporaryTreasurerPermission(request.getEventId(), uploadedBy);
        }

        Transaction transaction = new Transaction();
        transaction.setTitle(request.getTitle());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setEventId(request.getEventId());
        transaction.setUploadedBy(uploadedBy);

        if (file != null && !file.isEmpty()) {
            Map<String, Object> uploadResult = fileStorageService.uploadFile(file);
            transaction.setFileUrl((String) uploadResult.get("secure_url"));
            transaction.setPublicId((String) uploadResult.get("public_id"));
            transaction.setOriginalFileName(file.getOriginalFilename());
            transaction.setContentType(file.getContentType());
        }

        Transaction saved = transactionRepository.save(transaction);

        if (saved.getEventId() != null) {
            updateEventBalance(saved.getEventId());
        }

        auditService.logAction("CREATE_TRANSACTION", "TRANSACTION", saved.getId(), uploadedBy,
                "Amount: " + saved.getAmount() + " | Type: " + saved.getType());

        return mapToResponse(saved);
    }

    /**
     * FIXED: Better Temporary Treasurer Validation
     */
    private void validateTemporaryTreasurerPermission(String eventId, String username) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            return;
        }

        boolean isMainTreasurer = currentUser.getId().equals(event.getTreasurerId());
        boolean isTempTreasurer = event.getTemporaryTreasurerId() != null &&
                currentUser.getId().equals(event.getTemporaryTreasurerId());

        if (!isMainTreasurer && !isTempTreasurer) {
            throw new AccessDeniedException("You are not authorized for this event. " +
                    "Only the assigned Treasurer or Temporary Treasurer can manage it.");
        }
    }

    @Transactional
    public void softDeleteTransaction(String id, String deletedBy) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (transaction.isDeleted()) {
            throw new IllegalStateException("Transaction is already deleted");
        }

        transaction.softDelete();
        transactionRepository.save(transaction);

        if (transaction.getEventId() != null) {
            updateEventBalance(transaction.getEventId());
        }

        auditService.logAction("SOFT_DELETE_TRANSACTION", "TRANSACTION", id, deletedBy,
                "Deleted " + transaction.getType() + " | Amount: " + transaction.getAmount());

        System.out.println("✅ Transaction soft deleted successfully.");
    }

    @Transactional
    public void updateEventBalance(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        List<Transaction> transactions = transactionRepository.findByEventIdAndIsDeletedFalse(eventId);

        BigDecimal income = transactions.stream()
                .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = transactions.stream()
                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        event.setTotalIncome(income);
        event.setTotalExpense(expense);
        event.setTotalBalance(income.subtract(expense));

        eventRepository.save(event);
    }

    public TransactionResponse getTransactionById(String id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return mapToResponse(t);
    }

    public Page<TransactionResponse> getMainFundTransactionsPaginated(Pageable pageable) {
        return transactionRepository.findByEventIdIsNullAndIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    public Page<TransactionResponse> getEventTransactionsPaginated(String eventId, Pageable pageable) {
        return transactionRepository.findByEventIdAndIsDeletedFalse(eventId, pageable)
                .map(this::mapToResponse);
    }

    public Page<TransactionResponse> getTransactionsByUploadedByPaginated(String username, Pageable pageable) {
        return transactionRepository.findByUploadedByAndIsDeletedFalse(username, pageable)
                .map(this::mapToResponse);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        TransactionResponse response = new TransactionResponse();
        response.setId(t.getId());
        response.setTitle(t.getTitle());
        response.setAmount(t.getAmount());
        response.setType(t.getType());
        response.setCategory(t.getCategory());
        response.setDescription(t.getDescription());
        response.setFileUrl(t.getFileUrl());
        response.setOriginalFileName(t.getOriginalFileName());
        response.setEventId(t.getEventId());
        response.setUploadedBy(t.getUploadedBy());
        response.setCreatedAt(t.getCreatedAt());
        return response;
    }

    // For ReportService
    public List<TransactionResponse> getMainFundTransactionsByMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        java.util.Date startDate = java.util.Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
        java.util.Date endDate = java.util.Date.from(end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Transaction> transactions = transactionRepository
                .findByEventIdIsNullAndIsDeletedFalseAndCreatedAtBetween(startDate, endDate);

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}