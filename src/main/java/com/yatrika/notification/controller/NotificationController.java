package com.yatrika.notification.controller;

import com.yatrika.notification.domain.DeviceToken;
import com.yatrika.notification.domain.Notification;
import com.yatrika.notification.domain.NotificationPreference;
import com.yatrika.notification.repository.DeviceTokenRepository;
import com.yatrika.notification.repository.NotificationPreferenceRepository;
import com.yatrika.notification.repository.NotificationRepository;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @GetMapping
    public ResponseEntity<Page<Notification>> getMyNotifications(
            @RequestParam Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam Long userId) {
        return ResponseEntity.ok(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tokens")
    public ResponseEntity<Void> registerToken(@RequestParam Long userId, @RequestBody String token) {
        // 1. Check if token already exists
        if (!deviceTokenRepository.existsByToken(token)) {

            // 2. Find the user or throw an error if not found
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3. Build and save
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .token(token)
                    .deviceType("mobile")
                    .build();
            deviceTokenRepository.save(deviceToken);
        }
        return ResponseEntity.ok().build();
    }


    // Setting Preference
    @GetMapping("/settings")
    public ResponseEntity<NotificationPreference> getSettings(@RequestParam Long userId) {
        // Return existing preferences or a default set if they haven't customized yet
        NotificationPreference prefs = preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    var user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return NotificationPreference.builder()
                            .user(user)
                            .tripReminders(true)
                            .weatherAlerts(true)
                            .communityUpdates(true)
                            .build();
                });
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationPreference> updateSettings(
            @RequestParam Long userId,
            @RequestBody NotificationPreference newSettings) {

        NotificationPreference prefs = preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    var user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    newSettings.setUser(user);
                    return newSettings;
                });

        prefs.setTripReminders(newSettings.isTripReminders());
        prefs.setWeatherAlerts(newSettings.isWeatherAlerts());
        prefs.setCommunityUpdates(newSettings.isCommunityUpdates());

        return ResponseEntity.ok(preferenceRepository.save(prefs));
    }
}