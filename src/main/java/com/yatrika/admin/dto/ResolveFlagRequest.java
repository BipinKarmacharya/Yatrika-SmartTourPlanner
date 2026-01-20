package com.yatrika.admin.dto;

import com.yatrika.moderation.domain.ContentFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveFlagRequest {
    private ContentFlag.FlagStatus action; // APPROVED, REJECTED, REMOVED
    private String resolutionNote;
}