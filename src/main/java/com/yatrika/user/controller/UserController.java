package com.yatrika.user.controller;

import com.yatrika.user.domain.UserRole;
import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.service.CurrentUserService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management and Profile APIs")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    // --- 1. PROFILE SECTION (Authenticated User Context) ---

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(currentUserService.getCurrentProfile());
    }

    @PatchMapping(value = "/me/profile-image", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update profile picture", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateProfilePicture(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(currentUserService.updateProfileImage(file));
    }

    @PutMapping("/me/interests")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update current user interests", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateMyInterests(@RequestBody List<Long> interestIds) {
        UserResponse updated = currentUserService.updateInterests(interestIds);
        return ResponseEntity.ok(updated);
    }

    // --- 2. GENERAL USER SECTION (Identity Context) ---

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @currentUserService.getCurrentUserId() == #id")
    @Operation(summary = "Update user details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("@currentUserService.getCurrentUserId() == #id")
    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> changePassword(
            @PathVariable Long id,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        return ResponseEntity.ok(userService.changePassword(id, oldPassword, newPassword));
    }

    @PostMapping("/{followingId}/follow")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Follow/Unfollow a user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> toggleFollow(@PathVariable Long followingId) {
        userService.toggleFollow(currentUserService.getCurrentUserId(), followingId);
        return ResponseEntity.ok().build();
    }

    // --- 3. ADMIN ONLY SECTION ---

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user role", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> changeUserRole(@PathVariable Long id, @RequestParam UserRole role) {
        return ResponseEntity.ok(userService.changeUserRole(id, role));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle active status", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Long id, @RequestParam Boolean active) {
        return ResponseEntity.ok(userService.toggleUserActiveStatus(id, active));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user stats", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserStatsResponse> getUserStats() {
        return ResponseEntity.ok(new UserStatsResponse(
                userService.getTotalUserCount(),
                userService.getActiveUserCount()
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private record UserStatsResponse(Long totalUsers, Long activeUsers) {}
}


//package com.yatrika.user.controller;
//
//import com.yatrika.user.domain.User;
//import com.yatrika.user.domain.UserRole;
//import com.yatrika.user.dto.request.UpdateUserRequest;
//import com.yatrika.user.dto.request.UserPreferencesDTO;
//import com.yatrika.user.dto.response.UserResponse;
//import com.yatrika.user.service.CurrentUserService;
//import com.yatrika.user.service.UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/users")
//@RequiredArgsConstructor
//@Tag(name = "Users", description = "User management APIs")
//public class UserController {
//
//    private final UserService userService;
//    private final CurrentUserService currentUserService;
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN') or @currentUserService.getCurrentUserId() == #id")
//    @Operation(
//            summary = "Get user by ID",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
//        UserResponse response = userService.getUserById(id);
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{id}/interests")
//    public ResponseEntity<?> updateInterests(@PathVariable Long id, @RequestBody List<String> interests) {
//        try {
//            User updatedUser = userService.updateUserInterests(id, interests);
//            return ResponseEntity.ok(updatedUser);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Error updating interests: " + e.getMessage());
//        }
//    }
//
//    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN') or @currentUserService.getCurrentUserId() == #id")
//    @Operation(
//            summary = "Update user",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<UserResponse> updateUser(
//            @PathVariable Long id,
//            @Valid @RequestBody UpdateUserRequest request) {
//        UserResponse response = userService.updateUser(id, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(
//            summary = "Delete user (Admin only)",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
//        userService.deleteUser(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(
//            summary = "Get all users (Admin only)",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<Page<UserResponse>> getAllUsers(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//        Page<UserResponse> response = userService.getAllUsers(pageable);
//        return ResponseEntity.ok(response);
//    }
//
//    @PatchMapping("/{id}/role")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(
//            summary = "Change user role (Admin only)",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<UserResponse> changeUserRole(
//            @PathVariable Long id,
//            @RequestParam UserRole role) {
//        UserResponse response = userService.changeUserRole(id, role);
//        return ResponseEntity.ok(response);
//    }
//
//    @PatchMapping("/{id}/status")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(
//            summary = "Toggle user active status (Admin only)",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<UserResponse> toggleUserStatus(
//            @PathVariable Long id,
//            @RequestParam Boolean active) {
//        UserResponse response = userService.toggleUserActiveStatus(id, active);
//        return ResponseEntity.ok(response);
//    }
//
//    @PatchMapping("/{id}/password")
//    @PreAuthorize("@currentUserService.getCurrentUserId() == #id")
//    @Operation(
//            summary = "Change password",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<UserResponse> changePassword(
//            @PathVariable Long id,
//            @RequestParam String oldPassword,
//            @RequestParam String newPassword) {
//        UserResponse response = userService.changePassword(id, oldPassword, newPassword);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/stats/count")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(
//            summary = "Get user statistics (Admin only)",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<UserStatsResponse> getUserStats() {
//        Long totalUsers = userService.getTotalUserCount();
//        Long activeUsers = userService.getActiveUserCount();
//
//        UserStatsResponse response = new UserStatsResponse(totalUsers, activeUsers);
//        return ResponseEntity.ok(response);
//    }
//
//    // Follow feature
//    @PostMapping("/{followingId}/follow")
//    @PreAuthorize("hasRole('USER')")
//    @Operation(
//            summary = "Follow or Unfollow a user",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<Void> toggleFollow(@PathVariable Long followingId) {
//        Long followerId = currentUserService.getCurrentUserId();
//        userService.toggleFollow(followerId, followingId);
//        return ResponseEntity.ok().build();
//    }
//
//    // Inner class for stats response
//    private record UserStatsResponse(Long totalUsers, Long activeUsers) {}
//}