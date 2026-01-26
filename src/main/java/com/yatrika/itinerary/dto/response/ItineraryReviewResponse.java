package com.yatrika.itinerary.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItineraryReviewResponse {
    private Long id;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
