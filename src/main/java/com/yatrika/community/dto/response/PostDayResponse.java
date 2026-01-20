package com.yatrika.community.dto.response;

import lombok.Data;

@Data
public class PostDayResponse {
    private Long id;
    private Integer dayNumber;
    private String description;
    private String activities;
    private String accommodation;
    private String food;
    private String transportation;
}