// com.yatrika.admin.controller.AdminController.java
package com.yatrika.admin.controller;

import com.yatrika.admin.dto.*;
import com.yatrika.admin.service.AdminService;
import com.yatrika.user.domain.User;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminService adminService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Get system statistics")
    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getSystemStats() {
        StatsDTO stats = adminService.getSystemStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get users with filtering")
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDTO>> getUsers(
            @Valid @ModelAttribute UserFilterDTO filter) {
        Page<AdminUserDTO> users = adminService.getUsers(filter);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Update user")
    @PutMapping("/users/{userId}")
    public ResponseEntity<AdminUserDTO> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        AdminUserDTO updatedUser = adminService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Get user growth analytics")
    @GetMapping("/analytics/user-growth")
    public ResponseEntity<List<UserGrowthDTO>> getUserGrowth(
            @RequestParam(defaultValue = "30") int days) {
        List<UserGrowthDTO> growthData = adminService.getUserGrowth(days);
        return ResponseEntity.ok(growthData);
    }

    @Operation(summary = "Get popular destinations")
    @GetMapping("/analytics/popular-destinations")
    public ResponseEntity<List<PopularDestinationDTO>> getPopularDestinations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30") int days) {
        List<PopularDestinationDTO> destinations = adminService.getPopularDestinations(limit, days);
        return ResponseEntity.ok(destinations);
    }

    @Operation(summary = "Get activity by hour")
    @GetMapping("/analytics/activity-times")
    public ResponseEntity<List<ActivityTimeDTO>> getActivityTimes(
            @RequestParam(defaultValue = "7") int days) {
        List<ActivityTimeDTO> activityData = adminService.getActivityByHour(days);
        return ResponseEntity.ok(activityData);
    }

    @Operation(summary = "Get content flags")
    @GetMapping("/content-flags")
    public ResponseEntity<Page<ContentFlagDTO>> getContentFlags(
            @Valid @ModelAttribute FlagFilterDTO filter) {
        Page<ContentFlagDTO> flags = adminService.getContentFlags(filter);
        return ResponseEntity.ok(flags);
    }

    @Operation(summary = "Resolve content flag")
    @PostMapping("/content-flags/{flagId}/resolve")
    public ResponseEntity<ContentFlagDTO> resolveContentFlag(
            @PathVariable Long flagId,
            @Valid @RequestBody ResolveFlagRequest request) {
        User admin = currentUserService.getCurrentUserEntity();
        ContentFlagDTO resolvedFlag = adminService.resolveContentFlag(flagId, request, admin);
        return ResponseEntity.ok(resolvedFlag);
    }
}