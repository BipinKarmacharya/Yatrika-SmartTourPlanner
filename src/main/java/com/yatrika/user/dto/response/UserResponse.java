package com.yatrika.user.dto.response;

import com.yatrika.interest.dto.response.InterestResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private Boolean isEmailVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String fullName;
    private String profileImage;
    private Long followerCount;
    private Long followingCount;
    private List<InterestResponse> interests;
}