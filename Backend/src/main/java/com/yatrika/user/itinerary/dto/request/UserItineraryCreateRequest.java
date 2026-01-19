package com.yatrika.user.itinerary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class UserItineraryCreateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long userId; // Current logged-in user

    private LocalDate startDate;
    private LocalDate endDate;

    private List<UserItineraryItemRequest> items;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<UserItineraryItemRequest> getItems() { return items; }
    public void setItems(List<UserItineraryItemRequest> items) { this.items = items; }
}
