package com.yatrika.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostMediaRequest {
    @NotBlank(message = "Media URL is required")
    private String mediaUrl;

    @NotBlank(message = "Media type is required")
    private String mediaType; // image, video

    private String caption;

    private Integer dayNumber;

    private Integer displayOrder;
}
