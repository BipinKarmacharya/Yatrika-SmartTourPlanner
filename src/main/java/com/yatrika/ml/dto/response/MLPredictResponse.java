package com.yatrika.ml.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class MLPredictResponse {
    private String city;
    private Integer days;
    private Integer total_pois;
    private List<DailyPlan> daily_plans;
    private String message;
}
