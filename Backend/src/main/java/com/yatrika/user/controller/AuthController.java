package com.yatrika.user.controller;

import com.yatrika.user.dto.request.LoginRequest;
import com.yatrika.user.dto.request.RegisterRequest;
import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.response.AuthResponse;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.service.AuthService;
import com.yatrika.user.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = currentUserService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = currentUserService.updateCurrentUser(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        currentUserService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }
}