// EventResponse.java
package com.batch.treasury_management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EventResponse {
    private String id;
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;
    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private String treasurerId;
    private String temporaryTreasurerId;
    private Date createdAt;
}