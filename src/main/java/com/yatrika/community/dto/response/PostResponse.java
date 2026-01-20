package com.yatrika.community.dto.response;

import com.yatrika.user.dto.response.UserResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private Long id;
    private UserResponse user;
    private String title;
    private String destination;
    private List<String> tags;
    private String content;
    private Integer tripDurationDays;
    private Double estimatedCost;
    private String coverImageUrl;
    private Boolean isPublic;
    private Integer totalViews;
    private Integer totalLikes;
    private Boolean isLikedByCurrentUser;
    private List<PostMediaResponse> media;
    private List<PostDayResponse> days;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



