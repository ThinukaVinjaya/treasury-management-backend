package com.batch.treasury_management.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardSummary {
    private BigDecimal mainFundBalance = BigDecimal.ZERO;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private int totalEvents;
    private int totalUsers;

    // For Charts
    private List<MonthlyIncomeExpense> monthlyTrend;
    private List<CategoryBreakdown> expenseByCategory;
}