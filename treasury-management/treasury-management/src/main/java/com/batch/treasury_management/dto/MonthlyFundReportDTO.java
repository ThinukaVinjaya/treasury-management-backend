// src/main/java/com/batch/treasury_management/dto/MonthlyFundReportDTO.java
package com.batch.treasury_management.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
public class MonthlyFundReportDTO {
    private YearMonth month;
    private BigDecimal monthlyIncome = BigDecimal.ZERO;
    private BigDecimal monthlyExpense = BigDecimal.ZERO;
    private BigDecimal monthlyContributions = BigDecimal.ZERO;
    private BigDecimal netBalance = BigDecimal.ZERO;

    private List<ContributionResponse> paidContributions;
    private List<UserResponse> unpaidUsers;
}