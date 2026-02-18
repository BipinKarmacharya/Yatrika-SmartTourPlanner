package com.yatrika.user.mapper;

import com.yatrika.interest.domain.Interest;
import com.yatrika.interest.domain.UserInterest;
import com.yatrika.interest.dto.response.InterestResponse;
import com.yatrika.interest.mapper.InterestMapper;
import com.yatrika.user.domain.User;
import com.yatrika.user.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {InterestMapper.class})
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "profileImage", source = "profileImageUrl")
    @Mapping(target = "role", source = "role", defaultValue = "USER")
    @Mapping(target = "followerCount", ignore = true)
    @Mapping(target = "followingCount", ignore = true)
    @Mapping(target = "interests", source = "userInterests") // This points to the mapping below
    UserResponse toUserResponse(User user);

    // ✅ MapStruct will automatically use this for each item in the list
    // It extracts the Interest entity from the UserInterest link table
    default InterestResponse mapUserInterestToResponse(UserInterest userInterest) {
        if (userInterest == null || userInterest.getInterest() == null) {
            return null;
        }
        // Extract the actual interest and map it
        Interest interest = userInterest.getInterest();
        return InterestResponse.builder()
                .id(interest.getId())   // This ensures ID is 1, 2, 3 (The Interest ID)
                .name(interest.getName())
                .code(interest.getCode())
                .icon(interest.getIcon())
                .build();
    }
}