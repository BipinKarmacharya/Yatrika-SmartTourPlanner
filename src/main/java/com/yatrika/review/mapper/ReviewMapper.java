package com.yatrika.review.mapper;

import com.yatrika.review.domain.Review;
import com.yatrika.review.dto.response.ReviewResponse;
import com.yatrika.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ReviewMapper {

    @Mapping(target = "user", source = "user")
    @Mapping(target = "destinationId", source = "destination.id")
    ReviewResponse toResponse(Review review);
}