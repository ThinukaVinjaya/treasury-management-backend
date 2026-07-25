package com.batch.treasury_management.service;

import com.batch.treasury_management.entity.Contribution;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.repository.ContributionRepository;
import com.batch.treasury_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContributionReminderService {

    private final ContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Send Monthly Contribution Reminders
     * Every 10th of the month at 9:00 AM Sri Lanka Time (Asia/Colombo)
     */
    @Scheduled(cron = "0 0 9 10 * ?", zone = "Asia/Colombo")
    public void sendMonthlyReminders() {
        YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Colombo"));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Colombo"));

        log.info("Running monthly contribution reminder on {}", today);

        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> !u.isDeleted())
                .toList();

        int sentCount = 0;

        for (User user : activeUsers) {
            try {
                boolean hasPaid = hasPaidForMonth(user.getId(), currentMonth);

                if (!hasPaid) {
                    emailService.sendContributionReminder(
                            user.getEmail(),
                            user.getFullName(),
                            currentMonth.toString()
                    );
                    sentCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send reminder to {}", user.getEmail(), e);
            }
        }

        log.info("Monthly reminders sent to {} users for {}", sentCount, currentMonth);
    }

    /**
     * Checks if user has paid contribution for a specific month (Main Fund)
     */
    private boolean hasPaidForMonth(String userId, YearMonth month) {
        return contributionRepository
                .existsByUserIdAndMonthAndEventIdIsNullAndIsPaidTrue(userId, month);
    }

    /**
     * Optional: Send reminders for a specific event
     */
    public void sendEventContributionReminders(String eventId) {
        YearMonth currentMonth = YearMonth.now();

        List<User> users = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> !u.isDeleted())
                .toList();

        for (User user : users) {
            boolean hasPaid = contributionRepository
                    .existsByUserIdAndMonthAndEventIdAndIsPaidTrue(user.getId(), currentMonth, eventId);

            if (!hasPaid) {
                emailService.sendSimpleEmail(
                        user.getEmail(),
                        "Event Contribution Reminder - " + currentMonth,
                        "Dear " + user.getFullName() + ",\n\nYou have a pending contribution for this event."
                );
            }
        }
    }
}