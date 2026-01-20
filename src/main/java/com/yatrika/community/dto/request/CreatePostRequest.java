package com.yatrika.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String destination;

    private String content;

    private List<String> tags;

    private Integer tripDurationDays;

    private Double estimatedCost;

    private String coverImageUrl;

    @NotNull
    private Boolean isPublic = true;

    private List<PostMediaRequest> media;

    private List<PostDayRequest> days;
}



