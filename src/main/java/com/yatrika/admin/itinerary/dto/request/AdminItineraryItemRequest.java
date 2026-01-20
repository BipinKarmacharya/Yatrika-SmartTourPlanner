package com.yatrika.admin.itinerary.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class AdminItineraryItemRequest {

    @NotNull(message = "Destination ID is required")
    private Long destinationId;

    @NotNull(message = "Day number is required")
    @Min(value = 1, message = "Day number must be at least 1")
    private Integer dayNumber;

    @NotNull(message = "Order in day is required")
    @Min(value = 0, message = "Order in day must be at least 0")
    private Integer orderInDay;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;

    @NotBlank(message = "Activity type is required")
    private String activityType;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private BigDecimal estimatedCost;
}
