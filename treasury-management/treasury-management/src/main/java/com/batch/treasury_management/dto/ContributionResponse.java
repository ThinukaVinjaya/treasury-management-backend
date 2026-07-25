package com.batch.treasury_management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Date;

@Data
public class ContributionResponse {
    private String id;
    private String userId;
    private YearMonth month;
    private BigDecimal amount;
    private boolean isPaid;
    private String eventId;
    private Date createdAt;

    private String transactionId;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}