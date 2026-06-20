package com.batch.treasury_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String action;           // CREATE_TRANSACTION, ASSIGN_TEMP_TREASURER, etc.

    @Column(nullable = false)
    private String entityType;       // TRANSACTION, EVENT, CONTRIBUTION, USER

    private String entityId;

    private String performedBy;      // username

    @Column(length = 1000)
    private String details;

    private Date timestamp;
}