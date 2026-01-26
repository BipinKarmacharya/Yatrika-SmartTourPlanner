package com.yatrika.user.service;

import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.mapper.UserMapper;
import com.yatrika.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    public UserResponse getUserById(Long userId) {
        User user = getUserEntity(userId);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = getUserEntity(userId);

        // Check if email is being updated and is unique
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AppException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        // Check if username is being updated and is unique
        if (request.getUsername() != null && !user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new AppException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }

        // Update other fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated: {}", user.getId());

        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public User updateUserInterests(Long userId, List<String> interests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // JPA will handle the collection table update automatically
        user.setInterests(interests);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toUserResponse);
    }

    @Transactional
    public UserResponse changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserEntity(userId);

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException("Old password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);

        log.info("Password changed for user: {}", userId);
        return userMapper.toUserResponse(updatedUser);
    }

    // Admin-only methods
    @Transactional
    public UserResponse changeUserRole(Long userId, UserRole newRole) {
        User user = getUserEntity(userId);
        user.setRole(newRole);

        User updatedUser = userRepository.save(user);
        log.info("User role changed: {} -> {}", userId, newRole);

        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse toggleUserActiveStatus(Long userId, Boolean isActive) {
        User user = getUserEntity(userId);
        user.setIsActive(isActive);

        User updatedUser = userRepository.save(user);
        log.info("User active status changed: {} -> {}", userId, isActive);

        return userMapper.toUserResponse(updatedUser);
    }

    // Statistics
    public Long getTotalUserCount() {
        return userRepository.count();
    }

    public Long getActiveUserCount() {
        return userRepository.findAll().stream()
                .filter(User::getIsActive)
                .count();
    }
}