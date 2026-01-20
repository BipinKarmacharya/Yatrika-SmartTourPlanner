package com.yatrika.itinerary.mapper;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.dto.response.ItineraryItemResponse;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.user.mapper.UserMapper;
import com.yatrika.destination.mapper.DestinationMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, DestinationMapper.class})
public interface ItineraryMapper {

    @Mapping(target = "user", source = "user")
    @Mapping(target = "items", source = "items")
    ItineraryResponse toResponse(Itinerary itinerary);

    @Mapping(target = "destination", source = "destination")
    ItineraryItemResponse toItemResponse(ItineraryItem item);
}
