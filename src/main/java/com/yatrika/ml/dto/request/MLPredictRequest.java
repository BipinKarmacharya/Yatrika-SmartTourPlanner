package com.yatrika.ml.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MLPredictRequest {
    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Budget level is required")
    private BudgetLevel budget;

    @NotEmpty(message = "At least one interest is required")
    private List<String> interests;

    @Min(value = 1, message = "Days must be at least 1")
    @Max(value = 10, message = "Days must not be more than 10")
    private Integer days;

    private LocalDate startDate;

    public enum BudgetLevel {
        Low, Medium, High
    }
}
