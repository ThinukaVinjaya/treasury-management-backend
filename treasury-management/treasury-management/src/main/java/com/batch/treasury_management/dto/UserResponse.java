// UserResponse.java
package com.batch.treasury_management.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean isActive;
    private boolean isFirstLogin;
    private Date lastLoginAt;
    private Date createdAt;
}