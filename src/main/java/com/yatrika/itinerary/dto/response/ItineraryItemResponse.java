package com.yatrika.itinerary.dto.response;

import com.yatrika.destination.dto.response.DestinationResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class ItineraryItemResponse {
    private Long id;
    private Integer dayNumber;
    private Integer orderInDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String activityType;
    private String title;
    private String description;
    private String notes;
    private String locationName;
    private String locationAddress;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private Boolean isCompleted;
    private Boolean isCancelled;
    private DestinationResponse destination;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
