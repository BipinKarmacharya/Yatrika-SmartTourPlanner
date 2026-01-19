package com.yatrika.user.service;

import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.security.UserPrincipal;
import com.yatrika.user.domain.User;
import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.mapper.UserMapper;
import com.yatrika.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserRepository  userRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AppException("User not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }

    public User getCurrentUserEntity() {
        Long userId = getCurrentUserId();
        return userService.getUserEntity(userId);
    }

    public User getCurrentUserEntityOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return null; // Don't throw exception for guests
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId()).orElse(null);
    }

    public UserResponse getCurrentUser() {
        Long userId = getCurrentUserId();
        return userService.getUserById(userId);
    }

    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        Long userId = getCurrentUserId();
        return userService.updateUser(userId, request);
    }

    public void deleteCurrentUser() {
        Long userId = getCurrentUserId();
        userService.deleteUser(userId);
    }

    @Transactional
    public UserResponse updateProfileImage(String imageUrl) {
        User user = getCurrentUserEntity();
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }
}