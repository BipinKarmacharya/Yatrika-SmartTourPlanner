package com.yatrika.itinerary.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ItineraryResponse {
    private Long id;
    private String title;
    private String description;
    private Long userId;
    private String status;
    private Boolean isPublic;
    private Boolean isAdminCreated;
    private Long sourceId;
    private Integer totalDays;
    private String theme;
    private List<ItineraryItemResponse> items;
    private ItinerarySummary summary;
}