package com.yatrika.recommendation.dto;

import com.yatrika.itinerary.dto.response.ItineraryImageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItineraryRecommendationResponse {
    private Long id;
    private String title;
    private String description;
    private List<String> tags;
    private Integer totalDays;
    private BigDecimal estimatedBudget;
    private Double averageRating;
    private Integer likeCount;
    private Integer copyCount;
    private List<ItineraryImageResponse> images;// first image as cover
}
