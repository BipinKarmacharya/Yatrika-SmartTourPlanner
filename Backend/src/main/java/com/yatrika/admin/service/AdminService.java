// com.yatrika.admin.service.AdminService.java
package com.yatrika.admin.service;

import com.yatrika.admin.dto.*;
import com.yatrika.community.repository.PostRepository;
import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.moderation.domain.ContentFlag;
import com.yatrika.moderation.repository.ContentFlagRepository;
import com.yatrika.review.repository.ReviewRepository;
import com.yatrika.user.domain.User;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final UserRepository userRepository;
    private final DestinationRepository destinationRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final ContentFlagRepository contentFlagRepository;

    public StatsDTO getSystemStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long newUsersLast7Days = userRepository.countByCreatedAtBetween(
                LocalDateTime.now().minusDays(7), LocalDateTime.now());
        long destinationCount = destinationRepository.count();
        long postCount = postRepository.count();
        long reviewCount = reviewRepository.count();
        long pendingFlags = contentFlagRepository.countByStatus(ContentFlag.FlagStatus.PENDING);

        return StatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersLast7Days(newUsersLast7Days)
                .destinationCount(destinationCount)
                .postCount(postCount)
                .reviewCount(reviewCount)
                .pendingFlags(pendingFlags)
                .build();
    }

    public Page<AdminUserDTO> getUsers(UserFilterDTO filter) {
        Pageable pageable = createPageable(filter.getPage(), filter.getSize(),
                filter.getSortBy(), filter.getSortDirection());

        Page<User> users = userRepository.findUsersWithFilters(
                filter.getSearch(),
                filter.getRole(),
                filter.getActive(),
                pageable);

        return users.map(this::convertToAdminUserDTO);
    }

    @Transactional
    public AdminUserDTO updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getActive() != null) {
            user.setIsActive(request.getActive());
            if (!request.getActive()) {
                user.setDeactivatedAt(LocalDateTime.now());
                user.setDeactivationReason(request.getDeactivationReason());
            } else {
                user.setDeactivatedAt(null);
                user.setDeactivationReason(null);
            }
        }

        User savedUser = userRepository.save(user);
        return convertToAdminUserDTO(savedUser);
    }

    public List<UserGrowthDTO> getUserGrowth(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Object[]> results = userRepository.countNewUsersPerDay(startDate);

        List<UserGrowthDTO> growthData = new ArrayList<>();
        long cumulativeCount = 0;

        for (Object[] result : results) {
            LocalDate date = (LocalDate) result[0];
            Long count = (Long) result[1];
            cumulativeCount += count;

            growthData.add(UserGrowthDTO.builder()
                    .date(date)
                    .userCount(count)
                    .cumulativeCount(cumulativeCount)
                    .build());
        }

        return growthData;
    }

    public List<PopularDestinationDTO> getPopularDestinations(int limit, int days) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = destinationRepository.findPopularDestinationsSince(sinceDate);

        return results.stream()
                .limit(limit)
                .map(result -> {
                    Destination destination = (Destination) result[0];
                    Long itineraryCount = (Long) result[1];

                    double avgRating = destination.getAverageRating() != null
                            ? destination.getAverageRating().doubleValue()
                            : 0.0;

                    return PopularDestinationDTO.builder()
                            .destinationId(destination.getId())
                            .destinationName(destination.getName())
                            .location(destination.getLocationString())
                            .reviewCount(destination.getTotalReviews())
                            .itineraryCount(itineraryCount)
                            .averageRating(Math.round(avgRating * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<ActivityTimeDTO> getActivityByHour(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<ActivityTimeDTO> activityData = new ArrayList<>();

        // Initialize all hours
        for (int hour = 0; hour < 24; hour++) {
            activityData.add(ActivityTimeDTO.builder()
                    .hour(hour)
                    .postCount(0)
                    .reviewCount(0)
                    .totalActivity(0)
                    .build());
        }

        // Get post activity
        List<Object[]> postActivity = postRepository.countPostsByHour(startDate);
        for (Object[] result : postActivity) {
            int hour = (int) result[0];
            Long count = (Long) result[1];

            ActivityTimeDTO dto = activityData.get(hour);
            dto.setPostCount(count);
            dto.setTotalActivity(dto.getTotalActivity() + count);
        }

        // Get review activity
        List<Object[]> reviewActivity = reviewRepository.countReviewsByHour(startDate);
        for (Object[] result : reviewActivity) {
            int hour = (int) result[0];
            Long count = (Long) result[1];

            ActivityTimeDTO dto = activityData.get(hour);
            dto.setReviewCount(count);
            dto.setTotalActivity(dto.getTotalActivity() + count);
        }

        return activityData;
    }

    public Page<ContentFlagDTO> getContentFlags(FlagFilterDTO filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 20,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ContentFlag> flags = contentFlagRepository.findWithFilters(
                filter.getStatus(),
                filter.getContentType(),
                filter.getStartDate(),
                filter.getEndDate(),
                pageable);

        return flags.map(this::convertToContentFlagDTO);
    }

    @Transactional
    public ContentFlagDTO resolveContentFlag(Long flagId, ResolveFlagRequest request, User admin) {
        ContentFlag flag = contentFlagRepository.findById(flagId)
                .orElseThrow(() -> new RuntimeException("Flag not found"));

        flag.setStatus(request.getAction());
        flag.setResolvedBy(admin);
        flag.setResolvedAt(LocalDateTime.now());
        flag.setResolutionNote(request.getResolutionNote());

        ContentFlag savedFlag = contentFlagRepository.save(flag);
        return convertToContentFlagDTO(savedFlag);
    }

    private AdminUserDTO convertToAdminUserDTO(User user) {
        long pCount = postRepository.countByUserId(user.getId());
        long rCount = reviewRepository.countByUserId(user.getId());
        long iCount = 0; // itineraryRepository.countByOwnerId(user.getId());

        return AdminUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .postCount((int) pCount)
                .reviewCount((int) rCount)
                .itineraryCount((int) iCount)
                .build();
    }

    private ContentFlagDTO convertToContentFlagDTO(ContentFlag flag) {
        String contentPreview = "";
        if (flag.getContentType() == ContentFlag.ContentType.POST) {
            // You would fetch the post content here
            contentPreview = "Post ID: " + flag.getContentId();
        } else {
            contentPreview = "Review ID: " + flag.getContentId();
        }

        return ContentFlagDTO.builder()
                .id(flag.getId())
                .contentType(flag.getContentType())
                .contentId(flag.getContentId())
                .contentPreview(contentPreview)
                // Ensure getReporter() is not null before accessing
                .reporterId(flag.getReporter() != null ? flag.getReporter().getId() : null)
                .reporterUsername(flag.getReporter() != null ? flag.getReporter().getUsername() : "Unknown").reason(flag.getReason())
                .status(flag.getStatus())
                .createdAt(flag.getCreatedAt())
                .resolvedAt(flag.getResolvedAt())
                .resolvedById(flag.getResolvedBy() != null ? flag.getResolvedBy().getId() : null)
                .resolvedByUsername(flag.getResolvedBy() != null ? flag.getResolvedBy().getUsername() : null)
                .resolutionNote(flag.getResolutionNote())
                .build();
    }

    private Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        if (sortBy != null) {
            return PageRequest.of(pageNumber, pageSize, direction, sortBy);
        }
        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));
    }
}