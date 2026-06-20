package com.batch.treasury_management.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MonthlyIncomeExpense {
    private String month;
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;
}