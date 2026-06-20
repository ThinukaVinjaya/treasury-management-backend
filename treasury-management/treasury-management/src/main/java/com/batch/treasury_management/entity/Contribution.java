// src/main/java/com/batch/treasury_management/entity/Contribution.java
package com.batch.treasury_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "contributions")
public class Contribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private YearMonth month;  // e.g., 2026-06

    @Column(nullable = false)
    private BigDecimal amount;

    private boolean isPaid = false;

    private String eventId;  // null = Main monthly contribution

    private String transactionId; // Link to transaction if paid
}