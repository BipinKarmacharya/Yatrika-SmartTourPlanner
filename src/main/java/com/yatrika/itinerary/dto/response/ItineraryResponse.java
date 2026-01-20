package com.yatrika.itinerary.dto.response;

import com.yatrika.itinerary.domain.ItineraryStatus;
import com.yatrika.itinerary.domain.TripType;
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
    private String coverImageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private TripType tripType;
    private String budgetRange;
    private BigDecimal estimatedTotalCost;
    private BigDecimal actualTotalCost;
    private ItineraryStatus status;
    private Boolean isPublic;
    private Integer totalViews;
    private Integer totalLikes;
    private Integer totalBookmarks;
    private UserResponse user;
    private List<ItineraryItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}