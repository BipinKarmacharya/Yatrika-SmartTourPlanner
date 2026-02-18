package com.yatrika.recommendation.mapper;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.recommendation.dto.ItineraryRecommendationResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItineraryRecommendationMapper {
    ItineraryRecommendationResponse toResponse(Itinerary itinerary);
    List<ItineraryRecommendationResponse> toResponse(List<Itinerary> itineraries);
}
