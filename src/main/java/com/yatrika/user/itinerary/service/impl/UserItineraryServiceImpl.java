package com.yatrika.user.itinerary.service.impl;

import com.yatrika.admin.itinerary.domain.AdminItinerary;
import com.yatrika.admin.itinerary.domain.AdminItineraryItem;
import com.yatrika.admin.itinerary.repository.AdminItineraryRepository;
import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.user.itinerary.domain.UserItinerary;
import com.yatrika.user.itinerary.domain.UserItineraryItem;
import com.yatrika.user.itinerary.dto.request.UserItineraryCreateRequest;
import com.yatrika.user.itinerary.dto.request.UserItineraryItemRequest;
import com.yatrika.user.itinerary.dto.response.UserItineraryResponse;
import com.yatrika.user.itinerary.mapper.UserItineraryMapper;
import com.yatrika.user.itinerary.repository.UserItineraryItemRepository;
import com.yatrika.user.itinerary.repository.UserItineraryRepository;
import com.yatrika.user.itinerary.service.UserItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserItineraryServiceImpl implements UserItineraryService {

    private final UserItineraryRepository itineraryRepository;
    private final UserItineraryItemRepository itemRepository;
    private final DestinationRepository destinationRepository;
    private final AdminItineraryRepository adminItineraryRepository;
    private final UserItineraryMapper mapper;

    // ================= CREATE =================
    @Override
    public UserItineraryResponse createItinerary(UserItineraryCreateRequest request) {
        List<Long> destIds = request.getItems() != null
                ? request.getItems().stream().map(UserItineraryItemRequest::getDestinationId).distinct().toList()
                : List.of();
        var destinations = destinationRepository.findAllById(destIds);

        UserItinerary itinerary = mapper.toEntity(request, destinations);
        itineraryRepository.save(itinerary);

        return mapper.toResponse(itinerary);
    }

    // ================= READ =================
    @Override
    public UserItineraryResponse getItineraryById(Long id) {
        UserItinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User itinerary not found: " + id));
        return mapper.toResponse(itinerary);
    }

    @Override
    public List<UserItineraryResponse> getAllUserItineraries(Long userId) {
        return itineraryRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ================= UPDATE =================
    @Override
    public UserItineraryResponse updateItinerary(Long id, UserItineraryCreateRequest request) {
        UserItinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User itinerary not found: " + id));

        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setActive(true);

        // Handle items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            itinerary.getItems().clear();

            List<Long> destIds = request.getItems().stream()
                    .map(UserItineraryItemRequest::getDestinationId)
                    .distinct()
                    .toList();
            var destinations = destinationRepository.findAllById(destIds);

            for (UserItineraryItemRequest itemReq : request.getItems()) {
                var dest = destinations.stream()
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

        itineraryRepository.save(itinerary);
        return mapper.toResponse(itinerary);
    }

    // ================= SOFT DELETE =================
    @Override
    public void softDeleteItinerary(Long id) {
        UserItinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User itinerary not found: " + id));
        itinerary.setActive(false);
        itineraryRepository.save(itinerary);
    }

    // ================= UPDATE ITEM =================
    @Override
    public UserItineraryResponse updateItineraryItem(Long itemId, UserItineraryCreateRequest itemRequest) {
        UserItineraryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Itinerary item not found: " + itemId));

        if (!itemRequest.getItems().isEmpty()) {
            var reqItem = itemRequest.getItems().get(0); // expects one item for update
            item.setTitle(reqItem.getTitle());
            item.setNotes(reqItem.getNotes());
            item.setDayNumber(reqItem.getDayNumber());
            item.setOrderInDay(reqItem.getOrderInDay());
            item.setStartTime(reqItem.getStartTime());
            item.setEndTime(reqItem.getEndTime());
            item.setDurationMinutes(reqItem.getDurationMinutes());
            item.setActivityType(reqItem.getActivityType());
        }

        return mapper.toResponse(item.getUserItinerary());
    }

    // ================= DELETE ITEM =================
    @Override
    public UserItineraryResponse deleteItineraryItem(Long itemId) {
        UserItineraryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Itinerary item not found: " + itemId));
        UserItinerary itinerary = item.getUserItinerary();

        itinerary.getItems().remove(item);
        itemRepository.delete(item);

        return mapper.toResponse(itinerary);
    }


    // ================= Copy Admin Itinerary =================
    @Override
    public UserItineraryResponse copyAdminItinerary(Long adminItineraryId, Long userId) {

        AdminItinerary adminItinerary = adminItineraryRepository.findById(adminItineraryId)
                .orElseThrow(() -> new RuntimeException(
                        "Admin itinerary not found: " + adminItineraryId));

        List<Long> destIds = adminItinerary.getItems().stream()
                .map(item -> item.getDestination().getId())
                .distinct()
                .toList();

        List<Destination> destinations = destinationRepository.findAllById(destIds);

        UserItinerary userItinerary = UserItinerary.builder()
                .title(adminItinerary.getTitle())
                .description(adminItinerary.getDescription())
                .userId(userId)
                .active(true)
                .startDate(null)
                .endDate(null)
                .build();

        for (AdminItineraryItem adminItem : adminItinerary.getItems()) {
            Destination dest = destinations.stream()
                    .filter(d -> d.getId().equals(adminItem.getDestination().getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Destination not found: " + adminItem.getDestination().getId()));

            UserItineraryItem item = UserItineraryItem.builder()
                    .destination(dest)
                    .dayNumber(adminItem.getDayNumber())
                    .orderInDay(adminItem.getOrderInDay())
                    .startTime(adminItem.getStartTime())
                    .endTime(adminItem.getEndTime())
                    .durationMinutes(adminItem.getDurationMinutes())
                    .activityType(adminItem.getActivityType())
                    .title(adminItem.getTitle())
                    .notes(adminItem.getNotes())
                    .build();

            userItinerary.addItem(item);
        }

        itineraryRepository.save(userItinerary);
        return mapper.toResponse(userItinerary);
    }

}
