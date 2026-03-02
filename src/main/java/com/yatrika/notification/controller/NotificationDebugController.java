package com.yatrika.notification.controller;

import com.yatrika.notification.scheduler.NotificationScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/debug/notifications")
@RequiredArgsConstructor
public class NotificationDebugController {

    private final NotificationScheduler notificationScheduler;

    @PostMapping("/trigger-reminders")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Test Notification",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public String triggerReminders() {
        notificationScheduler.sendTripReminders();
        return "Reminder job triggered! Check logs and database.";
    }

    @PostMapping("/trigger-weather")
    public String triggerWeather() {
        notificationScheduler.checkWeatherForActiveTrips();
        return "Weather job triggered! Check logs and database.";
    }
}