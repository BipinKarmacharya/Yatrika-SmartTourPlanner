package com.yatrika.ml.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class MLSaveRequest {

    private String city;

    private int days;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private List<String> interests = new ArrayList<>();

    private Map<String, List<String>> itineraryData;
}