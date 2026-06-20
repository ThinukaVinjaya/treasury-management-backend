package com.batch.treasury_management.service;

import com.batch.treasury_management.dto.CategoryBreakdown;
import com.batch.treasury_management.dto.DashboardSummary;
import com.batch.treasury_management.dto.MonthlyIncomeExpense;
import com.batch.treasury_management.entity.Transaction;
import com.batch.treasury_management.repository.EventRepository;
import com.batch.treasury_management.repository.TransactionRepository;
import com.batch.treasury_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public DashboardSummary getDashboardSummary() {
        DashboardSummary summary = new DashboardSummary();

        var mainTransactions = transactionRepository.findByEventIdIsNullAndIsDeletedFalse();

        BigDecimal income = mainTransactions.stream()
                .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = mainTransactions.stream()
                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.setTotalIncome(income);
        summary.setTotalExpense(expense);
        summary.setMainFundBalance(income.subtract(expense));
        summary.setTotalEvents((int) eventRepository.count());
        summary.setTotalUsers((int) userRepository.count());

        // Fixed Monthly Trend
        summary.setMonthlyTrend(getRealMonthlyTrend(mainTransactions));

        // Expense by Category
        summary.setExpenseByCategory(getExpenseByCategory(mainTransactions));

        return summary;
    }

    private List<MonthlyIncomeExpense> getRealMonthlyTrend(List<Transaction> transactions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, MonthlyIncomeExpense> monthMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> {
                            LocalDate date = t.getCreatedAt().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            return date.format(formatter);
                        },
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    MonthlyIncomeExpense m = new MonthlyIncomeExpense();
                                    m.setMonth(list.get(0).getCreatedAt().toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate().format(formatter));
                                    m.setIncome(list.stream()
                                            .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
                                            .map(Transaction::getAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                                    m.setExpense(list.stream()
                                            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                                            .map(Transaction::getAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                                    return m;
                                }
                        )
                ));

        List<MonthlyIncomeExpense> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            String monthStr = date.format(formatter);

            MonthlyIncomeExpense m = monthMap.getOrDefault(monthStr, new MonthlyIncomeExpense());
            m.setMonth(monthStr);
            if (m.getIncome() == null) m.setIncome(BigDecimal.ZERO);
            if (m.getExpense() == null) m.setExpense(BigDecimal.ZERO);
            trend.add(m);
        }

        return trend;
    }

    private List<CategoryBreakdown> getExpenseByCategory(List<Transaction> transactions) {
        Map<String, BigDecimal> categoryMap = transactions.stream()
                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return categoryMap.entrySet().stream()
                .map(entry -> {
                    CategoryBreakdown cb = new CategoryBreakdown();
                    cb.setCategory(entry.getKey());
                    cb.setAmount(entry.getValue());
                    return cb;
                })
                .collect(Collectors.toList());
    }
}