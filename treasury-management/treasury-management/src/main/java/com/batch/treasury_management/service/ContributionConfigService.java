package com.batch.treasury_management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ContributionConfigService {

    @Value("${app.default-monthly-contribution:500}")
    private BigDecimal defaultMonthlyAmount;

    // In-memory value (will be updated via API)
    private BigDecimal currentMonthlyAmount;

    public BigDecimal getDefaultMonthlyContribution() {
        return currentMonthlyAmount != null ? currentMonthlyAmount : defaultMonthlyAmount;
    }

    public void updateDefaultMonthlyContribution(BigDecimal newAmount) {
        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        this.currentMonthlyAmount = newAmount;
        System.out.println("✅ Default monthly contribution updated to: " + newAmount);
    }
}