package com.yatrika.community.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdatePostRequest {
    private String title;
    private String content;
    private String coverImageUrl;
    private Boolean isPublic;
    private Integer tripDurationDays;
    private Double estimatedCost;
    private List<PostMediaRequest> media;
    private List<String> tags;
}