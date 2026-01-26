package com.yatrika.itinerary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ItineraryItemRequest {
    @NotNull(message = "Destination ID is required")
    private Long destinationId;

    @NotNull(message = "Day number is required")
    private Integer dayNumber;

    @NotNull(message = "Order is required")
    private Integer orderInDay;
    private BigDecimal estimatedCost;
    private String title; // User can override the destination name
    private String notes;
    private LocalTime startTime;
    private LocalTime endTime;
    private String activityType; // VISIT, MEAL, TRANSPORT
}