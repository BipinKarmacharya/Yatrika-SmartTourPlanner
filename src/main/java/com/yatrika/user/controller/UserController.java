package com.yatrika.user.controller;

import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.request.UserPreferencesDTO;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @currentUserService.getCurrentUserId() == #id")
    @Operation(
            summary = "Get user by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/interests")
    public ResponseEntity<?> updateInterests(@PathVariable Long id, @RequestBody List<String> interests) {
        try {
            User updatedUser = userService.updateUserInterests(id, interests);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating interests: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @currentUserService.getCurrentUserId() == #id")
    @Operation(
            summary = "Update user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete user (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all users (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> response = userService.getAllUsers(pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Change user role (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long id,
            @RequestParam UserRole role) {
        UserResponse response = userService.changeUserRole(id, role);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Toggle user active status (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        UserResponse response = userService.toggleUserActiveStatus(id, active);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("@currentUserService.getCurrentUserId() == #id")
    @Operation(
            summary = "Change password",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> changePassword(
            @PathVariable Long id,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        UserResponse response = userService.changePassword(id, oldPassword, newPassword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get user statistics (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserStatsResponse> getUserStats() {
        Long totalUsers = userService.getTotalUserCount();
        Long activeUsers = userService.getActiveUserCount();

        UserStatsResponse response = new UserStatsResponse(totalUsers, activeUsers);
        return ResponseEntity.ok(response);
    }

    // Inner class for stats response
    private record UserStatsResponse(Long totalUsers, Long activeUsers) {}
}