package com.yatrika.interest.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterestResponse {
    private Long id;
    private String code;
    private String name;
    private String icon;
}
