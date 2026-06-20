package com.batch.treasury_management.repository;

import com.batch.treasury_management.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByIsDeletedFalse();
    Page<Event> findByIsDeletedFalse(Pageable pageable);

    boolean existsByTemporaryTreasurerIdAndIsDeletedFalse(String temporaryTreasurerId);

    // ✅ Keep this one (for Temporary Treasurer check)
    boolean existsByIdAndTemporaryTreasurerIdAndIsDeletedFalse(String id, String temporaryTreasurerId);
}