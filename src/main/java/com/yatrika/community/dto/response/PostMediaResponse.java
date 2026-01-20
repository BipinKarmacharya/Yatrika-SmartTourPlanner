package com.yatrika.community.dto.response;

import lombok.Data;

@Data
public class PostMediaResponse {
    private Long id;
    private String mediaUrl;
    private String mediaType;
    private String caption;
    private Integer dayNumber;
    private Integer displayOrder;
}