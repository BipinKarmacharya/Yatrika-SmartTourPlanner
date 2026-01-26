package com.yatrika.itinerary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ItineraryRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String theme;
    private Integer totalDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal estimatedBudget;
}
