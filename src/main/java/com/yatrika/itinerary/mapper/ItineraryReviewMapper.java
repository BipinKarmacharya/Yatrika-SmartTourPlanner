package com.yatrika.itinerary.mapper;

import com.yatrika.itinerary.domain.ItineraryReview;
import com.yatrika.itinerary.dto.response.ItineraryReviewResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItineraryReviewMapper {
    ItineraryReviewResponse toResponse(ItineraryReview review);
}
