package com.batch.treasury_management.service;

import com.batch.treasury_management.entity.Contribution;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.repository.ContributionRepository;
import com.batch.treasury_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContributionScheduler {

    private final UserRepository userRepository;
    private final ContributionRepository contributionRepository;

    @Value("${app.default-monthly-contribution:500}")
    private BigDecimal defaultMonthlyAmount;

    @Scheduled(cron = "0 0 1 1 * ?") // 1st day of every month at 01:00 AM
    @Transactional
    public void createMonthlyContributions() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = YearMonth.of(2026, 6);

        // Don't create contributions before June 2026
        if (currentMonth.isBefore(startMonth)) {
            log.info("Monthly contributions not started yet. Starts from {}", startMonth);
            return;
        }

        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(user -> !user.isDeleted())
                .toList();

        int createdCount = 0;

        for (User user : activeUsers) {
            boolean exists = contributionRepository
                    .existsByUserIdAndMonthAndEventIdIsNull(user.getId(), currentMonth);

            if (!exists) {
                Contribution contribution = new Contribution();
                contribution.setUserId(user.getId());
                contribution.setMonth(currentMonth);
                contribution.setAmount(defaultMonthlyAmount);
                contribution.setPaid(false);
                contribution.setEventId(null);

                contributionRepository.save(contribution);
                createdCount++;
            }
        }

        log.info("✅ Monthly contributions auto-created for {} users for month: {}", createdCount, currentMonth);
    }
}