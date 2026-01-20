package com.yatrika.admin.dto;

import com.yatrika.moderation.domain.ContentFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentFlagDTO {
    private Long id;
    private ContentFlag.ContentType contentType;
    private Long contentId;
    private String contentPreview;
    private Long reporterId;
    private String reporterUsername;
    private String reason;
    private ContentFlag.FlagStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private Long resolvedById;
    private String resolvedByUsername;
    private String resolutionNote;
}
