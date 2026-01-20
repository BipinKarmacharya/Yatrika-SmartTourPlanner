package com.yatrika.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTimeDTO {
    private int hour;
    private long postCount;
    private long reviewCount;
    private long totalActivity;
}