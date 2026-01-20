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
public class FlagFilterDTO {
    private ContentFlag.ContentType contentType;
    private ContentFlag.FlagStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer page;
    private Integer size;
}