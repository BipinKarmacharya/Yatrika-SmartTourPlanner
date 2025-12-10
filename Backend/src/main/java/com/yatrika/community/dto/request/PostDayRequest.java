package com.yatrika.community.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostDayRequest {
    @NotNull(message = "Day number is required")
    private Integer dayNumber;

    private String description;

    private String activities;

    private String accommodation;

    private String food;

    private String transportation;
}
