package com.batch.treasury_management.entity;

import com.batch.treasury_management.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type;           // INCOME or EXPENSE

    @Column(nullable = false)
    private String category;

    private String description;

    // Cloudinary Fields
    private String fileUrl;
    private String publicId;
    private String originalFileName;
    private String contentType;

    private String eventId;        // null for Main Fund
    private String uploadedBy;
}