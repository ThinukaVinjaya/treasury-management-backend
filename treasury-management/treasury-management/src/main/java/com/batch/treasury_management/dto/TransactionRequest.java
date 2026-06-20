// TransactionRequest.java
package com.batch.treasury_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotBlank
    private String title;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String type; // INCOME or EXPENSE

    @NotBlank
    private String category;

    private String description;

    private String eventId; // null for main fund

    private String fileUrl;   // temporary for now

}