package com.yatrika.user.itinerary.mapper;

import com.yatrika.destination.domain.Destination;
import com.yatrika.user.itinerary.domain.UserItinerary;
import com.yatrika.user.itinerary.domain.UserItineraryItem;
import com.yatrika.user.itinerary.dto.request.UserItineraryCreateRequest;
import com.yatrika.user.itinerary.dto.request.UserItineraryItemRequest;
import com.yatrika.user.itinerary.dto.response.UserItineraryItemResponse;
import com.yatrika.user.itinerary.dto.response.UserItineraryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserItineraryMapper {

    // ================== CREATE ENTITY ==================
    public UserItinerary toEntity(UserItineraryCreateRequest request, List<Destination> destinations) {
        UserItinerary itinerary = UserItinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(request.getUserId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (UserItineraryItemRequest itemReq : request.getItems()) {
                Destination dest = destinations.stream()
                        .filter(d -> d.getId().equals(itemReq.getDestinationId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Destination not found: " + itemReq.getDestinationId()));

                UserItineraryItem item = UserItineraryItem.builder()
                        .destination(dest)
                        .dayNumber(itemReq.getDayNumber())
                        .orderInDay(itemReq.getOrderInDay())
                        .startTime(itemReq.getStartTime())
                        .endTime(itemReq.getEndTime())
                        .durationMinutes(itemReq.getDurationMinutes())
                        .activityType(itemReq.getActivityType())
                        .title(itemReq.getTitle())
                        .notes(itemReq.getNotes())
                        .build();

                itinerary.addItem(item);
            }
        }

        return itinerary;
    }

    // ================== ENTITY → RESPONSE ==================
    public UserItineraryResponse toResponse(UserItinerary itinerary) {
        UserItineraryResponse response = new UserItineraryResponse();
        response.setId(itinerary.getId());
        response.setTitle(itinerary.getTitle());
        response.setDescription(itinerary.getDescription());
        response.setUserId(itinerary.getUserId());
        response.setStartDate(itinerary.getStartDate());
        response.setEndDate(itinerary.getEndDate());
        response.setActive(itinerary.getActive());

        List<UserItineraryItemResponse> items = itinerary.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        response.setItems(items);

        return response;
    }

    // ================== ITEM → ITEM RESPONSE ==================
    public UserItineraryItemResponse toItemResponse(UserItineraryItem item) {
        UserItineraryItemResponse resp = new UserItineraryItemResponse();
        resp.setId(item.getId());
        resp.setDestinationId(item.getDestination().getId());
        resp.setDestinationName(item.getDestination().getName());
        resp.setDayNumber(item.getDayNumber());
        resp.setOrderInDay(item.getOrderInDay());
        resp.setStartTime(item.getStartTime());
        resp.setEndTime(item.getEndTime());
        resp.setDurationMinutes(item.getDurationMinutes());
        resp.setActivityType(item.getActivityType());
        resp.setTitle(item.getTitle());
        resp.setNotes(item.getNotes());
        return resp;
    }
}
