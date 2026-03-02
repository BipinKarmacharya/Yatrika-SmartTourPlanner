package com.yatrika.interest.mapper;

import com.yatrika.interest.domain.Interest;
import com.yatrika.interest.dto.response.InterestResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InterestMapper {

    InterestResponse toResponse(Interest interest);
}
