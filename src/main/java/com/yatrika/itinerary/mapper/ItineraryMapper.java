// ItineraryMapper.java - FIXED VERSION
package com.yatrika.itinerary.mapper;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.dto.response.ItineraryItemResponse;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.dto.response.ItinerarySummary;
import com.yatrika.destination.mapper.DestinationMapper;
import com.yatrika.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {DestinationMapper.class, UserMapper.class})
public abstract class ItineraryMapper {

    // Main mapping: Use MapStruct's built-in mapping for user
    @Mapping(target = "items", source = "items")
    @Mapping(target = "summary", expression = "java(calculateSummary(itinerary))")
    @Mapping(target = "user", source = "user") // Let MapStruct handle this

    public abstract ItineraryResponse toResponse(Itinerary itinerary);

    // Item mapping
    @Mapping(target = "isVisited", source = "isVisited")
    public abstract ItineraryItemResponse toItemResponse(ItineraryItem item);

    // Custom logic to build the summary object
    protected ItinerarySummary calculateSummary(Itinerary itinerary) {
        if (itinerary == null) return null;

        ItinerarySummary summary = new ItinerarySummary();
        summary.setTotalEstimatedBudget(itinerary.getEstimatedBudget());

        if (itinerary.getItems() != null && !itinerary.getItems().isEmpty()) {
            summary.setActivityCount(itinerary.getItems().size());

            // Count completed/visited activities
            long completed = itinerary.getItems().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIsVisited()))
                    .count();
            summary.setCompletedActivities(completed);

            // Group by activity type (Transport, Food, Sightseeing)
            Map<String, Long> breakdown = itinerary.getItems().stream()
                    .collect(Collectors.groupingBy(
                            item -> item.getActivityType() != null ? item.getActivityType() : "OTHER",
                            Collectors.counting()
                    ));
            summary.setActivityTypeBreakdown(breakdown);
        }

        return summary;
    }
}