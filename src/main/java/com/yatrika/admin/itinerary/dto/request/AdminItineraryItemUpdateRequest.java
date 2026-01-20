package com.yatrika.admin.itinerary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AdminItineraryItemUpdateRequest {
    @NotNull
    private String title;
    private String description;
    private Integer dayNumber;
    private Integer orderInDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String activityType;
}
