package com.yatrika.destination.dto.response;

import lombok.Data;

@Data
public class DestinationSummaryDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private Double averageRating;
    private String location;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
}
