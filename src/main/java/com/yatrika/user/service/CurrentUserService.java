package com.yatrika.user.service;

import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.security.UserPrincipal;
import com.yatrika.user.domain.User;
import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.mapper.UserMapper;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrentUserService implements UserProfileService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Override
    public UserResponse getCurrentProfile() {
        return userService.getUserById(getCurrentUserId());
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateUserRequest request) {
        return userService.updateUser(getCurrentUserId(), request);
    }

    @Override
    @Transactional
    public UserResponse updateProfileImage(MultipartFile file) {
        // This now correctly calls the Cloudinary logic in UserService
        return userService.updateProfileImage(getCurrentUserId(), file);
    }

    @Override
    @Transactional
    public UserResponse updateInterests(List<Long> interestIds) {
        return userService.updateUserInterests(getCurrentUserId(), interestIds);
    }


    // --- Helper Methods ---

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AppException("User not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }

    public User getCurrentUserEntity() {
        return userService.getUserEntity(getCurrentUserId());
    }

    public User getCurrentUserEntityOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return null;
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId()).orElse(null);
    }

    public void deleteCurrentUser() {
        userService.deleteUser(getCurrentUserId());
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}