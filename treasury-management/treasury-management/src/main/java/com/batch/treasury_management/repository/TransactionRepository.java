package com.batch.treasury_management.repository;

import com.batch.treasury_management.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByEventIdIsNullAndIsDeletedFalse();
    List<Transaction> findByEventIdAndIsDeletedFalse(String eventId);
    List<Transaction> findByUploadedByAndIsDeletedFalse(String uploadedBy);

    // ✅ Pagination Support
    Page<Transaction> findByEventIdIsNullAndIsDeletedFalse(Pageable pageable);
    Page<Transaction> findByEventIdAndIsDeletedFalse(String eventId, Pageable pageable);
    Page<Transaction> findByUploadedByAndIsDeletedFalse(String uploadedBy, Pageable pageable);
    // Add this method
    List<Transaction> findByEventIdIsNullAndIsDeletedFalseAndCreatedAtBetween(
            java.util.Date startDate, java.util.Date endDate);

    // Soft delete
    @Modifying
    @Query("UPDATE Transaction t SET t.isDeleted = true WHERE t.id = :id")
    void softDeleteById(@Param("id") String id);
}