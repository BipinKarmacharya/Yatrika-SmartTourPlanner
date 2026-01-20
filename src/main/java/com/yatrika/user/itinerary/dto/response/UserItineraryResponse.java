package com.yatrika.user.itinerary.dto.response;

import java.time.LocalDate;
import java.util.List;

public class UserItineraryResponse {

    private Long id;
    private String title;
    private String description;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;

    private List<UserItineraryItemResponse> items;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<UserItineraryItemResponse> getItems() { return items; }
    public void setItems(List<UserItineraryItemResponse> items) { this.items = items; }
}
