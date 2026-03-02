package com.yatrika.itinerary.dto.request;

import lombok.Data;

@Data
public class ItineraryImageRequest {
    private String url;
    private Integer sortOrder;
    private Boolean isCover;
}
