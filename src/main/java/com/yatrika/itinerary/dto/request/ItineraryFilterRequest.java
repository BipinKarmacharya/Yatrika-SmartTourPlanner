package com.yatrika.itinerary.dto.request;

import lombok.Data;

@Data
public class ItineraryFilterRequest {
    private String theme;         // Adventure, Cultural, etc.
    private String budgetRange;   // LOW, MEDIUM, HIGH
    private Integer minDays;
    private Integer maxDays;
    private String searchQuery;   // Search by title or description
}
