package com.yatrika.user.service;

import com.yatrika.user.dto.request.UpdateUserRequest;
import com.yatrika.user.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface UserProfileService {
    UserResponse getCurrentProfile();
    UserResponse updateProfile(UpdateUserRequest request);
    UserResponse updateProfileImage(MultipartFile file);
    UserResponse updateInterests(List<Long> interestIds);
}