// com.yatrika.admin.dto.StatsDTO.java
package com.yatrika.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsDTO {
    private long totalUsers;
    private long activeUsers;
    private long newUsersLast7Days;
    private long destinationCount;
    private long postCount;
    private long reviewCount;
    private long pendingFlags;
}
