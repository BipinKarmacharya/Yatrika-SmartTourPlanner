package com.yatrika.interest.dto.request;

import lombok.Data;

@Data
public class InterestRequest {
    private String code;
    private String name;
    private String icon;
    private Boolean active;
}