// TEMPORARY: Create simple test endpoint
package com.yatrika.destination.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public")
    public Map<String, String> publicTest() {
        return Map.of(
                "message", "Public endpoint works!",
                "access", "Guest access allowed"
        );
    }

    @GetMapping("/protected")
    public Map<String, String> protectedTest() {
        return Map.of(
                "message", "Protected endpoint works!",
                "access", "Authenticated users only"
        );
    }

    @GetMapping("/admin")
    public Map<String, String> adminTest() {
        return Map.of(
                "message", "Admin endpoint works!",
                "access", "Admin users only"
        );
    }
}