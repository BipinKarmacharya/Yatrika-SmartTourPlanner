package com.yatrika.itinerary.mapper;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.domain.UserLikedItinerary;
import com.yatrika.itinerary.dto.response.ItineraryItemResponse;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.dto.response.ItinerarySummary;
import com.yatrika.destination.mapper.DestinationMapper;
import com.yatrika.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {DestinationMapper.class, UserMapper.class})
public abstract class ItineraryMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "summary", expression = "java(calculateSummary(itinerary))")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "isLikedByCurrentUser", ignore = true)
    @Mapping(target = "isSavedByCurrentUser", ignore = true)
    public abstract ItineraryResponse toResponse(Itinerary itinerary);

    @Mapping(target = "isVisited", source = "isVisited")
    public abstract ItineraryItemResponse toItemResponse(ItineraryItem item);

    // Custom method to check if liked by current user
    @Named("checkIsLikedByUser")
    protected Boolean checkIsLikedByUser(Itinerary itinerary, Long currentUserId) {
        if (currentUserId == null || itinerary.getLikedByUsers() == null) {
            return false;
        }
        return itinerary.getLikedByUsers().stream()
                .anyMatch(like -> like.getUserId().equals(currentUserId));
    }

    // Custom method to check if saved by current user
    @Named("checkIsSavedByUser")
    protected Boolean checkIsSavedByUser(Itinerary itinerary, Long currentUserId) {
        if (currentUserId == null || itinerary.getSavedByUsers() == null) {
            return false;
        }
        return itinerary.getSavedByUsers().stream()
                .anyMatch(saved -> saved.getUserId().equals(currentUserId));
    }

    // Enhanced mapping with current user context
    public ItineraryResponse toResponse(Itinerary itinerary, Long currentUserId) {
        ItineraryResponse response = toResponse(itinerary);
        if (currentUserId != null) {
            response.setIsLikedByCurrentUser(checkIsLikedByUser(itinerary, currentUserId));
            response.setIsSavedByCurrentUser(checkIsSavedByUser(itinerary, currentUserId));
        } else {
            response.setIsLikedByCurrentUser(false);
            response.setIsSavedByCurrentUser(false);
        }
        return response;
    }

    protected ItinerarySummary calculateSummary(Itinerary itinerary) {
        if (itinerary == null) return null;

        ItinerarySummary summary = new ItinerarySummary();
        summary.setTotalEstimatedBudget(itinerary.getEstimatedBudget());

        if (itinerary.getItems() != null && !itinerary.getItems().isEmpty()) {
            summary.setActivityCount(itinerary.getItems().size());

            long completed = itinerary.getItems().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIsVisited()))
                    .count();
            summary.setCompletedActivities(completed);

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