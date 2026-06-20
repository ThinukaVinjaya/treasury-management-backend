// src/main/java/com/batch/treasury_management/entity/Event.java
package com.batch.treasury_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Date startDate;

    private Date endDate;

    @Column(nullable = false)
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    private String treasurerId;           // Main treasurer
    private String temporaryTreasurerId;  // Temporary treasurer for this event
}