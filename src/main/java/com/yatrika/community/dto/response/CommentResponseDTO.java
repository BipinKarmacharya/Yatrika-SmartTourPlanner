package com.yatrika.community.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponseDTO {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String userProfileImageUrl;
    private LocalDateTime createdAt;
}
