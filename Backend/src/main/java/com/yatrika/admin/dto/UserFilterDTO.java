package com.yatrika.admin.dto;

import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterDTO {
    private String search;
    private UserRole role;
    private Boolean active;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}