package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.UserRequest;
import com.batch.treasury_management.dto.UserResponse;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    /**
     * ✅ SUPER_ADMIN - Create New User (Treasurer or User)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody UserRequest request) {
        UserResponse response = userService.createUserResponse(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> users = userService.getAllUsersPaginated(pageable);

        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", userService.getUserById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> softDeleteUser(@PathVariable String id,
                                                              Authentication authentication) {
        String deletedBy = authentication.getName();
        userService.softDeleteUser(id, deletedBy);
        return ResponseEntity.ok(ApiResponse.success("User soft deleted successfully"));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetUserPassword(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> request) {

        String newPassword = request.get("newPassword");

        if (newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("New password must be at least 6 characters"));
        }

        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. User must change on next login."));
    }
}