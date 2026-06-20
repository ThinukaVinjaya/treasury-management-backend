package com.batch.treasury_management.controller;

import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.EventRepository;
import com.batch.treasury_management.repository.UserRepository;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.AuditService;
import com.batch.treasury_management.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/treasurer/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    /**
     * Test endpoint to send a single email
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> sendTestEmail(
            @RequestParam String to,
            Authentication authentication) {

        if (!isAuthorizedForBroadcast(authentication.getName())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You are not authorized to send emails"));
        }

        if (to == null || to.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email address is required"));
        }

        emailService.sendSimpleEmail(to, "Test Email from Treasury System",
                "Hello,\n\nThis is a test email from University Batch Treasury Management System.\n\nBest Regards,\nTreasury Team");

        auditService.logAction("SEND_TEST_EMAIL", "EMAIL", null,
                authentication.getName(), "Sent to: " + to);

        return ResponseEntity.ok(ApiResponse.success("Test email sent successfully to: " + to));
    }

    /**
     * Broadcast Email to ALL Active Users
     */
    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<String>> broadcastEmail(
            @RequestParam String subject,
            @RequestParam String message,
            Authentication authentication) {

        String username = authentication.getName();

        if (!isAuthorizedForBroadcast(username)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You are not authorized to send broadcast emails"));
        }

        if (subject == null || subject.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subject is required"));
        }

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Message is required"));
        }

        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(user -> !user.isDeleted())
                .collect(Collectors.toList());

        List<String> emails = activeUsers.stream().map(User::getEmail).collect(Collectors.toList());

        emailService.sendHtmlBroadcast(emails, subject, message);

        auditService.logAction("BROADCAST_EMAIL", "EMAIL", null, username,
                "HTML Broadcast sent to " + emails.size() + " users | Subject: " + subject);

        return ResponseEntity.ok(ApiResponse.success("Beautiful broadcast email sent to " + emails.size() + " active users"));
    }

    /**
     * Authorization Check: SUPER_ADMIN, TREASURER, or Temporary Treasurer
     */
    private boolean isAuthorizedForBroadcast(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if ("SUPER_ADMIN".equals(user.getRole()) || "TREASURER".equals(user.getRole())) {
            return true;
        }

        // Check if Temporary Treasurer for any active event
        return eventRepository.existsByTemporaryTreasurerIdAndIsDeletedFalse(user.getId());
    }
}