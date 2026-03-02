package com.yatrika.user.service;

import com.yatrika.interest.domain.Interest;
import com.yatrika.interest.domain.UserInterest;
import com.yatrika.interest.repository.InterestRepository;
import com.yatrika.interest.repository.UserInterestRepository;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.shared.service.CloudinaryStorageService;
import com.yatrika.subscription.domain.Subscription;
import com.yatrika.subscription.domain.SubscriptionTier;
import com.yatrika.subscription.repository.SubscriptionRepository;
import com.yatrika.user.domain.Follow;
import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.mapper.UserMapper;
import com.yatrika.user.repository.FollowRepository;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final InterestRepository interestRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Helper method to fetch a user's subscription or return a default FREE tier
     * if the record is missing.
     */
    private Subscription getSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> Subscription.builder()
                        .tier(SubscriptionTier.FREE)
                        .build());
    }

    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    public UserResponse getUserById(Long userId) {
        User user = getUserEntity(userId);
        // Updated: Passing both user and subscription to mapper
        UserResponse response = userMapper.toUserResponse(user, getSubscription(userId));

        // Ensure counts are updated in the response
        response.setFollowerCount(followRepository.countByFollowingId(userId));
        response.setFollowingCount(followRepository.countByFollowerId(userId));

        return response;
    }

    @Transactional
    public UserResponse updateProfileImage(Long userId, MultipartFile file) {
        User user = getUserEntity(userId);

        // 1. Upload to Cloudinary folder 'profiles'
        String imageUrl = cloudinaryStorageService.uploadFile(file, "profiles");

        // 2. Update and Save
        user.setProfileImageUrl(imageUrl);
        User updatedUser = userRepository.save(user);

        log.info("Profile image updated for user {}: {}", userId, imageUrl);
        // Updated: Passing both updatedUser and subscription
        return userMapper.toUserResponse(updatedUser, getSubscription(userId));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = getUserEntity(userId);

        // Check unique constraints for email and username if they are changing
        if (request.getEmail() != null && !user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AppException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getUsername() != null && !user.getUsername().equalsIgnoreCase(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new AppException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }

        // Update fields
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        User savedUser = userRepository.save(user);
        // Updated: Passing both savedUser and subscription
        return userMapper.toUserResponse(savedUser, getSubscription(userId));
    }

    @Transactional
    public UserResponse updateUserInterests(Long userId, List<Long> interestIds) {
        User user = getUserEntity(userId);

        // 1. Clear existing list
        user.getUserInterests().clear();

        // 2. IMPORTANT: Force Hibernate to execute the DELETE statements NOW
        userRepository.saveAndFlush(user);

        // 3. Fetch new Interest entities
        List<Interest> interests = interestRepository.findAllById(interestIds);

        // 4. Add new UserInterests
        interests.forEach(interest -> user.getUserInterests().add(
                UserInterest.builder()
                        .user(user)
                        .interest(interest)
                        .build()
        ));

        // 5. Save and return
        User savedUser = userRepository.save(user);
        // Updated: Passing both savedUser and subscription
        return userMapper.toUserResponse(savedUser, getSubscription(userId));
    }

    @Transactional
    public UserResponse changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserEntity(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);
        // Updated: Passing both savedUser and subscription
        return userMapper.toUserResponse(savedUser, getSubscription(userId));
    }

    @Transactional
    public void toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) throw new AppException("You cannot follow yourself");

        followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .ifPresentOrElse(
                        followRepository::delete,
                        () -> {
                            User follower = getUserEntity(followerId);
                            User following = getUserEntity(followingId);
                            followRepository.save(new Follow(follower, following));
                        }
                );
    }

    // --- Admin Section ---

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        // Updated: map method now fetches subscription for each user in the page
        return userRepository.findAll(pageable).map(user ->
                userMapper.toUserResponse(user, getSubscription(user.getId()))
        );
    }

    @Transactional
    public UserResponse changeUserRole(Long userId, UserRole role) {
        User user = getUserEntity(userId);
        user.setRole(role);
        User savedUser = userRepository.save(user);
        // Updated: Passing both savedUser and subscription
        return userMapper.toUserResponse(savedUser, getSubscription(userId));
    }

    @Transactional
    public UserResponse toggleUserActiveStatus(Long userId, Boolean isActive) {
        User user = getUserEntity(userId);
        user.setIsActive(isActive);
        User savedUser = userRepository.save(user);
        // Updated: Passing both savedUser and subscription
        return userMapper.toUserResponse(savedUser, getSubscription(userId));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        userRepository.deleteById(userId);
    }

    public Long getTotalUserCount() {
        return userRepository.count();
    }

    public Long getActiveUserCount() {
        return userRepository.countByIsActive(true);
    }
}






//package com.yatrika.user.service;
//
//import com.yatrika.interest.domain.Interest;
//import com.yatrika.interest.domain.UserInterest;
//import com.yatrika.interest.repository.InterestRepository;
//import com.yatrika.interest.repository.UserInterestRepository;
//import com.yatrika.shared.exception.AppException;
//import com.yatrika.shared.exception.ResourceNotFoundException;
//import com.yatrika.shared.service.CloudinaryStorageService;
//import com.yatrika.user.domain.Follow;
//import com.yatrika.user.domain.User;
//import com.yatrika.user.domain.UserRole;
//import com.yatrika.user.dto.request.UpdateUserRequest;
//import com.yatrika.user.dto.response.UserResponse;
//import com.yatrika.user.mapper.UserMapper;
//import com.yatrika.user.repository.FollowRepository;
//import com.yatrika.user.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final InterestRepository interestRepository;
//    private final UserInterestRepository userInterestRepository;
//    private final UserMapper userMapper;
//    private final PasswordEncoder passwordEncoder;
//    private final FollowRepository followRepository;
//    private final CloudinaryStorageService cloudinaryStorageService;
//
//    public User getUserEntity(Long userId) {
//        return userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
//    }
//
//    public UserResponse getUserById(Long userId) {
//        User user = getUserEntity(userId);
//        UserResponse response = userMapper.toUserResponse(user);
//
//        // Ensure counts are updated in the response
//        response.setFollowerCount(followRepository.countByFollowingId(userId));
//        response.setFollowingCount(followRepository.countByFollowerId(userId));
//
//        return response;
//    }
//
//    @Transactional
//    public UserResponse updateProfileImage(Long userId, MultipartFile file) {
//        User user = getUserEntity(userId);
//
//        // 1. Upload to Cloudinary folder 'profiles'
//        String imageUrl = cloudinaryStorageService.uploadFile(file, "profiles");
//
//        // 2. Update and Save
//        user.setProfileImageUrl(imageUrl);
//        User updatedUser = userRepository.save(user);
//
//        log.info("Profile image updated for user {}: {}", userId, imageUrl);
//        return userMapper.toUserResponse(updatedUser);
//    }
//
//    @Transactional
//    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
//        User user = getUserEntity(userId);
//
//        // Check unique constraints for email and username if they are changing
//        if (request.getEmail() != null && !user.getEmail().equalsIgnoreCase(request.getEmail())) {
//            if (userRepository.existsByEmail(request.getEmail())) {
//                throw new AppException("Email already in use");
//            }
//            user.setEmail(request.getEmail());
//        }
//
//        if (request.getUsername() != null && !user.getUsername().equalsIgnoreCase(request.getUsername())) {
//            if (userRepository.existsByUsername(request.getUsername())) {
//                throw new AppException("Username already taken");
//            }
//            user.setUsername(request.getUsername());
//        }
//
//        // Update fields
//        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
//        if (request.getLastName() != null) user.setLastName(request.getLastName());
//        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
//
//        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
//
//        return userMapper.toUserResponse(userRepository.save(user));
//    }
//
//    @Transactional
//    public UserResponse updateUserInterests(Long userId, List<Long> interestIds) {
//        User user = getUserEntity(userId);
//
//        // 1. Clear existing list
//        user.getUserInterests().clear();
//
//        // 2. IMPORTANT: Force Hibernate to execute the DELETE statements NOW
//        // This prevents the unique constraint violation when we add them back
//        userRepository.saveAndFlush(user);
//
//        // 3. Fetch new Interest entities
//        List<Interest> interests = interestRepository.findAllById(interestIds);
//
//        // 4. Add new UserInterests
//        interests.forEach(interest -> user.getUserInterests().add(
//                UserInterest.builder()
//                        .user(user)
//                        .interest(interest)
//                        .build()
//        ));
//
//        // 5. Save and return
//        User savedUser = userRepository.save(user);
//        return userMapper.toUserResponse(savedUser);
//    }
//
//
//    @Transactional
//    public UserResponse changePassword(Long userId, String oldPassword, String newPassword) {
//        User user = getUserEntity(userId);
//        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
//            throw new AppException("Old password is incorrect");
//        }
//        user.setPassword(passwordEncoder.encode(newPassword));
//        return userMapper.toUserResponse(userRepository.save(user));
//    }
//
//    //    @Transactional
////    public void toggleFollow(Long followerId, Long followingId) {
////        if (followerId.equals(followingId)) {
////            throw new AppException("You cannot follow yourself");
////        }
////
////        Optional<Follow> existing = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
////
////        if (existing.isPresent()) {
////            followRepository.delete(existing.get());
////            log.info("User {} unfollowed user {}", followerId, followingId);
////        } else {
////            User follower = getUserEntity(followerId);
////            User following = getUserEntity(followingId);
////            followRepository.save(Follow.builder()
////                    .follower(follower)
////                    .following(following)
////                    .build());
////            log.info("User {} followed user {}", followerId, followingId);
////        }
////    }
//    @Transactional
//    public void toggleFollow(Long followerId, Long followingId) {
//        if (followerId.equals(followingId)) throw new AppException("You cannot follow yourself");
//
//        followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
//                .ifPresentOrElse(
//                        followRepository::delete,
//                        () -> {
//                            User follower = getUserEntity(followerId);
//                            User following = getUserEntity(followingId);
//                            followRepository.save(new Follow(follower, following));
//                        }
//                );
//    }
//
//    // --- Admin Section ---
//
//    public Page<UserResponse> getAllUsers(Pageable pageable) {
//        return userRepository.findAll(pageable).map(userMapper::toUserResponse);
//    }
//
//    @Transactional
//    public UserResponse changeUserRole(Long userId, UserRole role) {
//        User user = getUserEntity(userId);
//        user.setRole(role);
//        return userMapper.toUserResponse(userRepository.save(user));
//    }
//
//    @Transactional
//    public UserResponse toggleUserActiveStatus(Long userId, Boolean isActive) {
//        User user = getUserEntity(userId);
//        user.setIsActive(isActive);
//        return userMapper.toUserResponse(userRepository.save(user));
//    }
//
//    @Transactional
//    public void deleteUser(Long userId) {
//        if (!userRepository.existsById(userId)) {
//            throw new ResourceNotFoundException("User", "id", userId);
//        }
//        userRepository.deleteById(userId);
//    }
//
//    public Long getTotalUserCount() {
//        return userRepository.count();
//    }
//
//    public Long getActiveUserCount() {
//        // Updated to use the derived query method usually found in Spring Data JPA
//        return userRepository.countByIsActive(true);
//    }
//}
////package com.yatrika.user.service;
////
////import com.yatrika.shared.exception.AppException;
////import com.yatrika.shared.exception.ResourceNotFoundException;
////import com.yatrika.user.domain.Follow;
////import com.yatrika.user.domain.User;
////import com.yatrika.user.domain.UserRole;
////import com.yatrika.user.dto.request.UpdateUserRequest;
////import com.yatrika.user.dto.response.UserResponse;
////import com.yatrika.user.mapper.UserMapper;
////import com.yatrika.user.repository.FollowRepository;
////import com.yatrika.user.repository.UserRepository;
////import jakarta.transaction.Transactional;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.data.domain.Page;
////import org.springframework.data.domain.Pageable;
////import org.springframework.security.crypto.password.PasswordEncoder;
////import org.springframework.stereotype.Service;
////
////import java.util.List;
////import java.util.Optional;
////
////@Service
////@RequiredArgsConstructor
////@Slf4j
////public class UserService {
////
////    private final UserRepository userRepository;
////    private final UserMapper userMapper;
////    private final PasswordEncoder passwordEncoder;
////    private final FollowRepository followRepository;
////
////    public User getUserEntity(Long userId) {
////        return userRepository.findById(userId)
////                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
////    }
////
////    public UserResponse getUserById(Long userId) {
////        User user = getUserEntity(userId);
////        UserResponse response = userMapper.toUserResponse(user);
////
////        // Use the methods here!
////        response.setFollowerCount(followRepository.countByFollowingId(userId));
////        response.setFollowingCount(followRepository.countByFollowerId(userId));
////
////        return response;
////    }
////
////    @Transactional
////    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
////        User user = getUserEntity(userId);
////
////        // Check if email is being updated and is unique
////        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
////            if (userRepository.existsByEmail(request.getEmail())) {
////                throw new AppException("Email already in use");
////            }
////            user.setEmail(request.getEmail());
////        }
////
////        // Check if username is being updated and is unique
////        if (request.getUsername() != null && !user.getUsername().equals(request.getUsername())) {
////            if (userRepository.existsByUsername(request.getUsername())) {
////                throw new AppException("Username already taken");
////            }
////            user.setUsername(request.getUsername());
////        }
////
////        // Update other fields
////        if (request.getFirstName() != null) {
////            user.setFirstName(request.getFirstName());
////        }
////
////        if (request.getLastName() != null) {
////            user.setLastName(request.getLastName());
////        }
////
////        if (request.getPhoneNumber() != null) {
////            user.setPhoneNumber(request.getPhoneNumber());
////        }
////
////        if (request.getProfileImageUrl() != null) {
////            user.setProfileImageUrl(request.getProfileImageUrl());
////        }
////
////        User updatedUser = userRepository.save(user);
////        log.info("User updated: {}", user.getId());
////
////        return userMapper.toUserResponse(updatedUser);
////    }
////
////    @Transactional
////    public User updateUserInterests(Long userId, List<String> interests) {
////        User user = userRepository.findById(userId)
////                .orElseThrow(() -> new RuntimeException("User not found"));
////
////        // JPA will handle the collection table update automatically
////        user.setInterests(interests);
////        return userRepository.save(user);
////    }
////
////    @Transactional
////    public void deleteUser(Long userId) {
////        if (!userRepository.existsById(userId)) {
////            throw new ResourceNotFoundException("User", "id", userId);
////        }
////        userRepository.deleteById(userId);
////        log.info("User deleted: {}", userId);
////    }
////
////    public Page<UserResponse> getAllUsers(Pageable pageable) {
////        Page<User> users = userRepository.findAll(pageable);
////        return users.map(userMapper::toUserResponse);
////    }
////
////    @Transactional
////    public UserResponse changePassword(Long userId, String oldPassword, String newPassword) {
////        User user = getUserEntity(userId);
////
////        // Verify old password
////        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
////            throw new AppException("Old password is incorrect");
////        }
////
////        // Update password
////        user.setPassword(passwordEncoder.encode(newPassword));
////        User updatedUser = userRepository.save(user);
////
////        log.info("Password changed for user: {}", userId);
////        return userMapper.toUserResponse(updatedUser);
////    }
////
////    // Admin-only methods
////    @Transactional
////    public UserResponse changeUserRole(Long userId, UserRole newRole) {
////        User user = getUserEntity(userId);
////        user.setRole(newRole);
////
////        User updatedUser = userRepository.save(user);
////        log.info("User role changed: {} -> {}", userId, newRole);
////
////        return userMapper.toUserResponse(updatedUser);
////    }
////
////    @Transactional
////    public UserResponse toggleUserActiveStatus(Long userId, Boolean isActive) {
////        User user = getUserEntity(userId);
////        user.setIsActive(isActive);
////
////        User updatedUser = userRepository.save(user);
////        log.info("User active status changed: {} -> {}", userId, isActive);
////
////        return userMapper.toUserResponse(updatedUser);
////    }
////
////    // Statistics
////    public Long getTotalUserCount() {
////        return userRepository.count();
////    }
////
////    public Long getActiveUserCount() {
////        return userRepository.findAll().stream()
////                .filter(User::getIsActive)
////                .count();
////    }
////
////    // Follow User
////    public Long getFollowerCount(Long userId) {
////        return followRepository.countByFollowingId(userId);
////    }
////
////    public Long getFollowingCount(Long userId) {
////        return followRepository.countByFollowerId(userId);
////    }
////
////
////    @Transactional
////    public void toggleFollow(Long followerId, Long followingId) {
////        if (followerId.equals(followingId)) throw new AppException("You cannot follow yourself");
////
////        Optional<Follow> existing = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
////        if (existing.isPresent()) {
////            followRepository.delete(existing.get());
////        } else {
////            User follower = getUserEntity(followerId);
////            User following = getUserEntity(followingId);
////            followRepository.save(Follow.builder().follower(follower).following(following).build());
////        }
////    }
////}