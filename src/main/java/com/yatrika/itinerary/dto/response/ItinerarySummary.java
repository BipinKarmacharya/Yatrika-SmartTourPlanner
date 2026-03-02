package com.yatrika.itinerary.dto.response;

import com.yatrika.itinerary.domain.ActivityType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ItinerarySummary {
    private BigDecimal totalEstimatedBudget;
    private long activityCount;
    private long completedActivities;
    private Map<ActivityType, Long> activityTypeBreakdown;
}
