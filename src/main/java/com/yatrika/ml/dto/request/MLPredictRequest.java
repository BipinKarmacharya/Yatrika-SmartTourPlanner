package com.yatrika.ml.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class MLPredictRequest {
    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Budget level is required")
    private String budget;

    @NotEmpty(message = "At least one interest is required")
    private List<String> interests;

    @Min(value = 1, message = "Days must be at least 1")
    private Integer days;
}
