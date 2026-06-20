package com.batch.treasury_management.repository;

import com.batch.treasury_management.entity.Contribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, String> {

    List<Contribution> findByUserIdAndIsDeletedFalse(String userId);
    List<Contribution> findByEventIdAndIsDeletedFalse(String eventId);

    Page<Contribution> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);
    Page<Contribution> findByEventIdAndIsDeletedFalse(String eventId, Pageable pageable);
    Page<Contribution> findByUserIdAndEventIdAndIsDeletedFalse(String userId, String eventId, Pageable pageable);

    Optional<Contribution> findByUserIdAndMonthAndEventId(String userId, YearMonth month, String eventId);
    Optional<Contribution> findByUserIdAndMonthAndEventIdIsNull(String userId, YearMonth month);

    boolean existsByUserIdAndMonthAndEventIdIsNull(String userId, YearMonth month);
    boolean existsByUserIdAndMonthAndEventIdIsNullAndIsPaidTrue(String userId, YearMonth month);
    boolean existsByUserIdAndMonthAndEventIdAndIsPaidTrue(String userId, YearMonth month, String eventId);

    // ✅ CORRECT METHOD - Add this here
    List<Contribution> findByEventIdAndIsPaidFalseAndIsDeletedFalse(String eventId);

    // Add these two methods
    List<Contribution> findByEventIdIsNullAndMonthAndIsPaidTrueAndIsDeletedFalse(YearMonth month);

    @Modifying
    @Query("UPDATE Contribution c SET c.isDeleted = true WHERE c.id = :id")
    void softDeleteById(@Param("id") String id);
}