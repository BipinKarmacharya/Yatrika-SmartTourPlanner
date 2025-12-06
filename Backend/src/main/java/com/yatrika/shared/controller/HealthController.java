// src/main/java/com/yatrika/shared/controller/HealthController.java
package com.yatrika.shared.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "YATRIKA Backend",
                "version", "1.0.0",
                "timestamp", System.currentTimeMillis(),
                "features", Map.of(
                        "guestAccess", true,
                        "authentication", true,
                        "destinationManagement", true
                )
        );
    }
}