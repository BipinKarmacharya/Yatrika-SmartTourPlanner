package com.yatrika.itinerary.dto.response;

import com.yatrika.user.dto.response.UserResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItineraryResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private Boolean isPublic;
    private Boolean isAdminCreated;
    private Long sourceId;
    private String theme;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private BigDecimal estimatedBudget;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double averageRating;
    private Integer likeCount;
    private Integer copyCount;
    private Boolean isLikedByCurrentUser;
    private Boolean isSavedByCurrentUser;
    private String countryCode;
    private List<String> tags;
    private List<ItineraryItemResponse> items;
    private ItinerarySummary summary;
    private UserResponse user;
}