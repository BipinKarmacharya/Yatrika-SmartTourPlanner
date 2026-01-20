package com.yatrika.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGrowthDTO {
    private LocalDate date;
    private long userCount;
    private long cumulativeCount;
}