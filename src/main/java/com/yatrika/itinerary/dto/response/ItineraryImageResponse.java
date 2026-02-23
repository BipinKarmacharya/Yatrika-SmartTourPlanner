package com.yatrika.itinerary.dto.response;

import lombok.Data;

@Data
public class ItineraryImageResponse {
    private Long id;
    private String url;
    private Integer sortOrder;
    private Boolean isCover;
}

