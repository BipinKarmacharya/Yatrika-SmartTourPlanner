package com.yatrika.ml.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DailyPlan {
    private Integer day;
    private List<String> places;
}
