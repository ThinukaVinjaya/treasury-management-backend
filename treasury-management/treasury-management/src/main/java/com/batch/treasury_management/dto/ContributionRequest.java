package com.batch.treasury_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class ContributionRequest {
    @NotBlank
    private String userId;

    @NotNull
    private YearMonth month;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String eventId; // null = main fund
}