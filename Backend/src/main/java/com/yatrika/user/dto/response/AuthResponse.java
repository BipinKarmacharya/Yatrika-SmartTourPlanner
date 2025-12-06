package com.yatrika.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @Builder.Default
    private String tokenType = "Bearer";
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}
