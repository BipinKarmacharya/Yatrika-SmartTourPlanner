package com.yatrika.community.mapper;

import com.yatrika.community.domain.Post;
import com.yatrika.community.domain.PostDay;
import com.yatrika.community.domain.PostMedia;
import com.yatrika.community.dto.response.PostDayResponse;
import com.yatrika.community.dto.response.PostMediaResponse;
import com.yatrika.community.dto.response.PostResponse;
import com.yatrika.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface PostMapper {

    @Mapping(target = "user", source = "user")
    @Mapping(target = "media", source = "media")
    @Mapping(target = "days", source = "days")
    @Mapping(target = "isLikedByCurrentUser", ignore = true)
    PostResponse toResponse(Post post);

    PostMediaResponse toMediaResponse(PostMedia media);

    PostDayResponse toDayResponse(PostDay day);

    @Named("toPostResponseList")
    List<PostResponse> toPostResponseList(List<Post> posts);
}