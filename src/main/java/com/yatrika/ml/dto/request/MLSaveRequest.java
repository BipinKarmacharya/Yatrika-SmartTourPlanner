package com.yatrika.ml.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MLSaveRequest {
    private MLPredictRequest metadata; // The city, budget, days info
    private Map<String, List<String>> itineraryData; // The "Day 1": ["Place A", "Place B"] map
}
