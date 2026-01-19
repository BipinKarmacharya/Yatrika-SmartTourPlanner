package com.yatrika.admin.dto;

import com.yatrika.user.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private UserRole role;
    private Boolean active;
    private String deactivationReason;
}