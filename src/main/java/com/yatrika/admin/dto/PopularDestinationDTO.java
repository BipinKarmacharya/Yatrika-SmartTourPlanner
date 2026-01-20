package com.yatrika.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularDestinationDTO {
    private Long destinationId;
    private String destinationName;
    private String location;
    private long reviewCount;
    private long itineraryCount;
    private double averageRating;
}