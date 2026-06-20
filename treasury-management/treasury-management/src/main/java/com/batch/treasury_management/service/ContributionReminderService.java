package com.batch.treasury_management.service;

import com.batch.treasury_management.entity.Contribution;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.repository.ContributionRepository;
import com.batch.treasury_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContributionReminderService {

    private final ContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public void sendMonthlyReminders() {
        YearMonth currentMonth = YearMonth.now();

        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (!user.isActive()) continue;

            // Check if user has paid for current month (main fund)
            boolean hasPaid = hasPaidForMonth(user.getId(), currentMonth);

            if (!hasPaid) {
                emailService.sendContributionReminder(
                        user.getEmail(),
                        user.getFullName(),
                        currentMonth.toString()
                );
            }
        }
    }

    /**
     * Checks if user has paid contribution for a specific month
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

        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (!user.isActive()) continue;

            boolean hasPaid = contributionRepository
                    .existsByUserIdAndMonthAndEventIdAndIsPaidTrue(user.getId(), currentMonth, eventId);

            if (!hasPaid) {
                emailService.sendSimpleEmail(
                        user.getEmail(),
                        "Event Contribution Reminder",
                        "Dear " + user.getFullName() + ",\n\nYou have a pending contribution for this event."
                );
            }
        }
    }
}