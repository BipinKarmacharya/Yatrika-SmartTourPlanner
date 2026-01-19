package com.yatrika.admin.itinerary.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AdminItineraryResponse {

    private Long id;
    private String title;
    private String description;
    private String theme;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private List<AdminItineraryItemResponse> items;
}
