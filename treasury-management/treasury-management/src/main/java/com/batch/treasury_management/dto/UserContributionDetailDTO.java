package com.batch.treasury_management.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class UserContributionDetailDTO {
    private String userId;
    private String username;
    private String fullName;
    private YearMonth month;
    private BigDecimal amount;
    private boolean paid;
    private String status; // "PAID", "UNPAID", "NOT GENERATED"
}