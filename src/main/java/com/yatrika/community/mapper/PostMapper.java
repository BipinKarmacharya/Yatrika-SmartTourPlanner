package com.yatrika.community.mapper;

import com.yatrika.community.domain.Post;
import com.yatrika.community.domain.PostDay;
import com.yatrika.community.domain.PostMedia;
import com.yatrika.community.dto.response.PostDayResponse;
import com.yatrika.community.dto.response.PostMediaResponse;
import com.yatrika.community.dto.response.PostResponse;
import com.yatrika.user.domain.User;
import com.yatrika.user.dto.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring") // Removed UserMapper.class to break dependency
public interface PostMapper {

    @Mapping(target = "user", source = "post.user")
    @Mapping(target = "isLikedByCurrentUser", ignore = true)
    PostResponse toResponse(Post post);

    // MapStruct will use this to convert the User entity to UserSummaryResponse
    UserSummaryResponse toUserSummaryResponse(User user);

    PostMediaResponse toMediaResponse(PostMedia media);

    PostDayResponse toDayResponse(PostDay day);

    List<PostResponse> toPostResponseList(List<Post> posts);
}