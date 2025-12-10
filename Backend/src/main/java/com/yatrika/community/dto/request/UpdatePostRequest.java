package com.yatrika.community.dto.request;

import lombok.Data;

@Data
public class UpdatePostRequest {
    private String title;
    private String content;
    private String coverImageUrl;
    private Boolean isPublic;
}