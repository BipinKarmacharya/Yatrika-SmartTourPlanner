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
    private Long userId;
    private String status;
    private Boolean isPublic;
    private Boolean isAdminCreated;
    private Long sourceId;
    private Integer totalDays;
    private String theme;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private BigDecimal estimatedBudget;
    private Double averageRating;
    private Integer likeCount;
    private Integer copyCount;
    private String countryCode;
    private List<String> tags;
    private List<ItineraryItemResponse> items;
    private ItinerarySummary summary;
    private UserResponse user;
}