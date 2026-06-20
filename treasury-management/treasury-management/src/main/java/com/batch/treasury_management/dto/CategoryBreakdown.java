package com.batch.treasury_management.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategoryBreakdown {
    private String category;
    private BigDecimal amount = BigDecimal.ZERO;
}