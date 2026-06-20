package com.batch.treasury_management.entity;

import com.batch.treasury_management.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;           // SUPER_ADMIN, TREASURER, USER

    private boolean isActive = true;
    private boolean isFirstLogin = true;

    private Date lastLoginAt;

    // ==================== OTP Fields for Password Reset ====================
    @Column(name = "password_reset_otp")
    private String passwordResetOtp;

    @Column(name = "otp_expiry_date")
    private LocalDateTime otpExpiryDate;

}