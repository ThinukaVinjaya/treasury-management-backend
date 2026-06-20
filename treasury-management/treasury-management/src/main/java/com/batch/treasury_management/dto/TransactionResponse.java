// TransactionResponse.java
package com.batch.treasury_management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TransactionResponse {
    private String id;
    private String title;
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;
    private String fileUrl;
    private String originalFileName;
    private String eventId;
    private String uploadedBy;
    private Date createdAt;
}