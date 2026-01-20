package com.yatrika.user.mapper;

import com.yatrika.user.domain.User;
import com.yatrika.user.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "role", source = "role", defaultValue = "USER")
    UserResponse toUserResponse(User user);
}
