package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.ChangePasswordRequest;
import com.batch.treasury_management.dto.ForgotPasswordRequest;
import com.batch.treasury_management.dto.LoginRequest;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.security.JwtTokenProvider;
import com.batch.treasury_management.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    // ==================== NORMAL LOGIN ====================
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.ok(ApiResponse.success("Login successful", token));
    }

    // ==================== NORMAL CHANGE PASSWORD (with old password) ====================
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePasswordWithOldPassword(
                request.getUsername(),
                request.getOldPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    // ==================== FORGOT PASSWORD FLOW ====================

    /**
     * Step 1: Request OTP for Forgot Password
     */
    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<ApiResponse<String>> requestForgotPasswordOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username is required"));
        }

        String message = userService.generateAndSendForgotPasswordOtp(username);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Step 2: Reset Password using OTP (No old password needed)
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponse<String>> resetPasswordWithOtp(@RequestBody ForgotPasswordRequest request) {
        userService.resetPasswordWithOtp(
                request.getUsername(),
                request.getOtp(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now login with new password."));
    }
}