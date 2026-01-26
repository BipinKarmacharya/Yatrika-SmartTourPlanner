package com.yatrika.itinerary.dto.response;

import com.yatrika.destination.dto.response.DestinationSummaryDTO;
import lombok.Data;
import java.time.LocalTime;

@Data
public class ItineraryItemResponse {
    private Long id;
    private Integer dayNumber;
    private Integer orderInDay;
    private String title;
    private String notes;
    private String activityType;
    private LocalTime startTime;
    private LocalTime endTime;
    private DestinationSummaryDTO destination;
}