package com.yatrika.user.dto.response;

import lombok.Data;

@Data
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String profileImageUrl;
}
