package com.yatrika.destination.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class NearbySearchRequest {
    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    private Double radiusKm = 10.0; // Default 10km radius
    private Integer limit = 20;
}
