package com.yatrika.user.itinerary.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public class UserItineraryItemRequest {

    @NotNull
    private Long destinationId;

    @NotNull
    private Integer dayNumber;

    @NotNull
    private Integer orderInDay;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;

    @Size(max = 50)
    private String activityType;

    @NotNull
    @Size(max = 100)
    private String title;

    private String notes;

    // Getters and Setters
    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public Integer getOrderInDay() { return orderInDay; }
    public void setOrderInDay(Integer orderInDay) { this.orderInDay = orderInDay; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
