package com.yatrika.admin.itinerary.mapper;

import com.yatrika.admin.itinerary.domain.AdminItinerary;
import com.yatrika.admin.itinerary.domain.AdminItineraryItem;
import com.yatrika.admin.itinerary.dto.request.AdminItineraryCreateRequest;
import com.yatrika.admin.itinerary.dto.request.AdminItineraryItemRequest;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryResponse;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryItemResponse;
import com.yatrika.destination.domain.Destination;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AdminItineraryMapper {

    public AdminItinerary toEntity(AdminItineraryCreateRequest request, List<Destination> destinations) {
        long daysBetween = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        AdminItinerary itinerary = AdminItinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .totalDays((int) daysBetween)
                .active(request.getActive() != null ? request.getActive() : true)
                .estimatedBudget(BigDecimal.ZERO)
                .build();

        // Map destinations for quick lookup
        Map<Long, Destination> destMap = destinations.stream()
                .collect(Collectors.toMap(Destination::getId, d -> d));

        BigDecimal totalBudget = BigDecimal.ZERO;

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (AdminItineraryItemRequest itemReq : request.getItems()) {
                Destination dest = destinations.stream()
                        .filter(d -> d.getId().equals(itemReq.getDestinationId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Destination not found: " + itemReq.getDestinationId()));

                AdminItineraryItem item = AdminItineraryItem.builder()
                        .destination(dest)
                        .dayNumber(itemReq.getDayNumber())
                        .orderInDay(itemReq.getOrderInDay())
                        .startTime(itemReq.getStartTime())
                        .endTime(itemReq.getEndTime())
                        .durationMinutes(itemReq.getDurationMinutes())
                        .activityType(itemReq.getActivityType())
                        .title(itemReq.getTitle())
                        .notes(itemReq.getDescription())
                        .build();

                if (itemReq.getEstimatedCost() != null) {
                    totalBudget = totalBudget.add(itemReq.getEstimatedCost());
                }

                itinerary.addItem(item);
            }
        }
        itinerary.setEstimatedBudget(totalBudget);
        return itinerary;
    }

    public AdminItineraryResponse toResponse(AdminItinerary itinerary) {
        AdminItineraryResponse response = new AdminItineraryResponse();
        response.setId(itinerary.getId());
        response.setTitle(itinerary.getTitle());
        response.setDescription(itinerary.getDescription());
        response.setTheme(itinerary.getTheme());
        response.setActive(itinerary.getActive());

        List<AdminItineraryItemResponse> items = itinerary.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        response.setItems(items);
        return response;
    }

    public AdminItineraryItemResponse toItemResponse(AdminItineraryItem item) {
        AdminItineraryItemResponse resp = new AdminItineraryItemResponse();
        resp.setId(item.getId());
        resp.setDayNumber(item.getDayNumber());
        resp.setOrderInDay(item.getOrderInDay());
        resp.setStartTime(item.getStartTime());
        resp.setEndTime(item.getEndTime());
        resp.setDurationMinutes(item.getDurationMinutes());
        resp.setActivityType(item.getActivityType());
        resp.setTitle(item.getTitle());
        resp.setDescription(item.getNotes());
        resp.setDestinationId(item.getDestination().getId());
        resp.setDestinationName(item.getDestination().getName());
        return resp;
    }
}
