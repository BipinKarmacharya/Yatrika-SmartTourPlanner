package com.yatrika.user.service;

import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.security.JwtTokenProvider;
import com.yatrika.shared.security.UserPrincipal;
import com.yatrika.subscription.domain.Subscription;
import com.yatrika.subscription.domain.SubscriptionTier;
import com.yatrika.subscription.repository.SubscriptionRepository;
import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import com.yatrika.user.dto.request.ForgotPasswordRequest;
import com.yatrika.user.dto.request.LoginRequest;
import com.yatrika.user.dto.request.RegisterRequest;
import com.yatrika.user.dto.request.ResetPasswordRequest;
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

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final org.springframework.mail.javamail.JavaMailSender mailSender;

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

        // 2. Create and Save the default FREE subscription for the new user
        Subscription subscription = Subscription.builder()
                .user(user)
                .tier(SubscriptionTier.FREE)
                .monthlyPlanCount(0)
                .cycleStartDate(java.time.LocalDateTime.now())
                .build();

        subscription = subscriptionRepository.save(subscription);

        log.info("User registered with default FREE subscription: {}", user.getEmail());

        // 3. Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserResponse userResponse = userMapper.toUserResponse(user, subscription);

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

        // Fetch subscription (default to FREE if not found)
        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> Subscription.builder().tier(SubscriptionTier.FREE).build());

        // Pass both to mapper
        UserResponse userResponse = userMapper.toUserResponse(user, subscription);

        log.info("User logged in successfully: {}{}", userResponse.getTier(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwt)
                .user(userResponse)
                .build();
    }


    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest request) {
        log.info("Generating OTP for email: {}", request.getEmail());

        // 1. Find the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("No account found with this email address."));

        // 2. If found, generate OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        user.setResetToken(otp);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // 3. Send the email
        sendOtpEmail(user.getEmail(), otp);
        log.info("OTP sent successfully to: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("Invalid request"));

        boolean isValidOtp = user.getResetToken() != null &&
                user.getResetToken().equals(request.getOtp()) &&
                user.getResetTokenExpiry().isAfter(LocalDateTime.now());

        if (!isValidOtp) {
            throw new AppException("Invalid or expired OTP code");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset successful for user: {}", user.getEmail());
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your Yatrika Reset Code");
            message.setText("Your password reset code is: " + otp + "\nThis code expires in 10 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Mail error: {}", e.getMessage());
        }
    }
}