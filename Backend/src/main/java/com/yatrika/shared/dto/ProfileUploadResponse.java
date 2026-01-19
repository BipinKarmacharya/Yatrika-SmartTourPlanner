package com.yatrika.shared.dto;

import com.yatrika.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUploadResponse {
    private String fileName;
    private String fileUrl;
    private String fileType;
    private long size;
    private UserResponse updatedUser;
}