package com.yatrika.itinerary.dto.response;

import com.yatrika.destination.dto.response.DestinationSummaryDTO;
import com.yatrika.itinerary.domain.ActivityType;
import lombok.Data;
import java.time.LocalTime;

@Data
public class ItineraryItemResponse {
    private Long id;
    private Integer dayNumber;
    private Integer orderInDay;
    private String title;
    private String notes;
    private ActivityType activityType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isVisited;
    private String imageUrl;
    private DestinationSummaryDTO destination;
}