package com.yatrika.user.service;

import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.security.JwtTokenProvider;
import com.yatrika.shared.security.UserPrincipal;
import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import com.yatrika.user.dto.request.LoginRequest;
import com.yatrika.user.dto.request.RegisterRequest;
import com.yatrika.user.dto.response.AuthResponse;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.mapper.UserMapper;
import com.yatrika.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already in use");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException("Username already taken");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(UserRole.USER)
                .isActive(true)
                .isEmailVerified(false)
                .build();
        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        //Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserResponse userResponse = userMapper.toUserResponse(user);

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(userResponse)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmailOrUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmailOrUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new AppException("User not found"));

        // Update last login
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(userResponse)
                .build();
    }

    /**
     * Retrieves the authenticated User entity.
     * Use this method when you need the full JPA entity (e.g., for setting foreign keys).
     *
     * @return The User entity for the currently logged-in user.
     * @throws AppException if the user is not authenticated or not found.
     */
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if authentication is null or principal is not UserPrincipal
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AppException("User not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new AppException("User entity not found for ID: " + userPrincipal.getId()));
    }

    /**
     * Retrieves the authenticated user as a DTO.
     *
     * @return The UserResponse DTO.
     */
    public UserResponse getCurrentUser() {
        User user = getCurrentUserEntity();
        return userMapper.toUserResponse(user);
    }
}