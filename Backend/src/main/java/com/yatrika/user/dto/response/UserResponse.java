package com.yatrika.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponse {
    private String id;
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
}
