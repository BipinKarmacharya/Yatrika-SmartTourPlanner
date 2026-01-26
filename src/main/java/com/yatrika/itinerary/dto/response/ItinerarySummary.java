package com.yatrika.itinerary.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ItinerarySummary {
    private BigDecimal totalEstimatedBudget;
    private long activityCount;
    private long completedActivities;
    private Map<String, Long> activityTypeBreakdown;
}
