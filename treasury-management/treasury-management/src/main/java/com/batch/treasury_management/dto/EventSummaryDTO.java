// src/main/java/com/batch/treasury_management/dto/EventSummaryDTO.java
package com.batch.treasury_management.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class EventSummaryDTO {
    private String id;
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;
    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalContributions;
    private String treasurerId;
    private String temporaryTreasurerId;
    private Date createdAt;

    // For detailed view
    private List<TransactionResponse> transactions;
    private List<ContributionResponse> contributions;
}