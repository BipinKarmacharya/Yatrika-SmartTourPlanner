// com.yatrika.moderation.dto.FlagContentRequest.java
package com.yatrika.moderation.dto;

import com.yatrika.moderation.domain.ContentFlag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagContentRequest {
    @NotNull
    private ContentFlag.ContentType contentType;

    @NotNull
    private Long contentId;

    @NotBlank
    private String reason;
}