package com.yatrika.destination.dto.response;

import com.yatrika.destination.domain.DestinationType;
import com.yatrika.destination.domain.DifficultyLevel;
import com.yatrika.review.dto.response.ReviewResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DestinationResponse {
    private Long id;
    private String name;
    private String shortDescription;
    private String description;
    private String district;
    private String province;
    private String locationString;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private DestinationType type;
    private String category;
    private DifficultyLevel difficultyLevel;
    private Integer averageDurationHours;
    private BigDecimal entranceFeeLocal;
    private BigDecimal entranceFeeForeign;
    private boolean freeEntry;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer popularityScore;
    private String[] tags;
    private Boolean hasParking;
    private Boolean hasRestrooms;
    private Boolean hasDrinkingWater;
    private Boolean hasWifi;
    private Boolean hasGuideServices;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReviewResponse> reviews;
}