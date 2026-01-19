package com.yatrika.admin.itinerary.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class AdminItineraryItemResponse {

    private Long id;
    private Integer dayNumber;
    private Integer orderInDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String activityType;
    private String title;
    private String description;
    private BigDecimal estimatedCost;
    private Long destinationId;
    private String destinationName;
}
