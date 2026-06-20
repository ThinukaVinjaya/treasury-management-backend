package com.batch.treasury_management.service;

import com.batch.treasury_management.dto.UserRequest;
import com.batch.treasury_management.dto.UserResponse;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.ContributionRepository;
import com.batch.treasury_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ContributionRepository contributionRepository;
    private final EmailService emailService;

    @Transactional
    public User createUser(UserRequest request) {
        // Username & Email uniqueness check
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Role handling with default
        String role = (request.getRole() != null ? request.getRole().toUpperCase().trim() : "USER");

        if (!List.of("USER", "TREASURER", "SUPER_ADMIN").contains(role)) {
            throw new IllegalArgumentException("Invalid role. Allowed: USER, TREASURER, SUPER_ADMIN");
        }

        // ✅ Allow only the FIRST SUPER_ADMIN (for DataInitializer)
        if ("SUPER_ADMIN".equals(role) && userRepository.count() > 0) {
            throw new IllegalArgumentException("Cannot create another SUPER_ADMIN user");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(true);
        user.setFirstLogin(true);

        User savedUser = userRepository.save(user);

        auditService.logAction("CREATE_USER", "USER", savedUser.getId(),
                "SYSTEM", "Role: " + role + " | Username: " + savedUser.getUsername());

        return savedUser;
    }

    public UserResponse createUserResponse(UserRequest request) {
        User user = createUser(request);
        return mapToResponse(user);
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<UserResponse> getAllUsersPaginated(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void softDeleteUser(String userId, String deletedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.softDelete();
        userRepository.save(user);

        auditService.logAction("SOFT_DELETE_USER", "USER", userId, deletedBy,
                "User soft deleted: " + user.getUsername());
    }

    @Transactional
    public void changePasswordWithOldPassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false);
        userRepository.save(user);

        auditService.logAction("PASSWORD_CHANGED", "USER", user.getId(), username,
                "Password changed using old password");
    }

    @Transactional
    public void resetPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(true);
        userRepository.save(user);

        auditService.logAction("RESET_PASSWORD", "USER", userId, "SUPER_ADMIN",
                "Password reset for user: " + user.getUsername());
    }

    // ==================== Forgot Password Flow ====================

    public String generateAndSendForgotPasswordOtp(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        String otp = String.format("%06d", new Random().nextInt(999999));

        user.setPasswordResetOtp(otp);
        user.setOtpExpiryDate(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendForgotPasswordOtp(user.getEmail(), user.getFullName(), otp);

        auditService.logAction("FORGOT_PASSWORD_OTP", "USER", user.getId(), username,
                "OTP sent to email");

        return "Verification code has been sent to your registered email.";
    }

    @Transactional
    public void resetPasswordWithOtp(String username, String otp, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (user.getPasswordResetOtp() == null || !user.getPasswordResetOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        if (user.getOtpExpiryDate() == null || user.getOtpExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false);
        user.setPasswordResetOtp(null);
        user.setOtpExpiryDate(null);

        userRepository.save(user);

        auditService.logAction("PASSWORD_RESET_OTP", "USER", user.getId(), username,
                "Password reset successfully via OTP");
    }

    // ==================== Helper Methods ====================

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setFirstLogin(user.isFirstLogin());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public List<UserResponse> getUnpaidUsersForMonth(YearMonth month) {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(user -> !user.isDeleted())
                .filter(user -> !hasPaidForMonth(user.getId(), month))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private boolean hasPaidForMonth(String userId, YearMonth month) {
        return contributionRepository
                .existsByUserIdAndMonthAndEventIdIsNullAndIsPaidTrue(userId, month);
    }
}