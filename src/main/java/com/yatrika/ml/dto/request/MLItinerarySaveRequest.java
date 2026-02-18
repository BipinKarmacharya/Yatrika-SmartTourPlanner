package com.yatrika.ml.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MLItinerarySaveRequest {
    private String city;
    private String budget;
    private List<String> interests;
    private Integer days;
    private Map<String, List<String>> generatedItinerary; // The "Day 1", "Day 2" data
}
