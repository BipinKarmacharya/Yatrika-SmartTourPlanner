package com.yatrika.admin.itinerary.service.impl;

import com.yatrika.admin.itinerary.domain.AdminItinerary;
import com.yatrika.admin.itinerary.domain.AdminItineraryItem;
import com.yatrika.admin.itinerary.dto.request.AdminItineraryCreateRequest;
import com.yatrika.admin.itinerary.dto.request.AdminItineraryItemRequest;
import com.yatrika.admin.itinerary.dto.request.AdminItineraryItemUpdateRequest;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryItemResponse;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryResponse;
import com.yatrika.admin.itinerary.mapper.AdminItineraryMapper;
import com.yatrika.admin.itinerary.repository.AdminItineraryItemRepository;
import com.yatrika.admin.itinerary.repository.AdminItineraryRepository;
import com.yatrika.admin.itinerary.service.AdminItineraryService;
import com.yatrika.destination.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminItineraryServiceImpl implements AdminItineraryService {

    private final AdminItineraryRepository itineraryRepository;
    private final AdminItineraryItemRepository itemRepository;
    private final DestinationRepository destinationRepository;
    private final AdminItineraryMapper mapper;

    // ================= CREATE =================
    @Override
    public AdminItineraryResponse createItinerary(AdminItineraryCreateRequest request) {
        List<Long> destIds = request.getItems() != null
                ? request.getItems().stream().map(AdminItineraryItemRequest::getDestinationId).distinct().toList()
                : List.of();
        var destinations = destinationRepository.findAllById(destIds);

        AdminItinerary itinerary = mapper.toEntity(request, destinations);
        itineraryRepository.save(itinerary);

        return mapper.toResponse(itinerary);
    }

    // ================= READ =================
    @Override
    public List<AdminItineraryResponse> getAllActiveItineraries() {
        return itineraryRepository.findByActiveTrue().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AdminItineraryResponse getItineraryById(Long id) {
        AdminItinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin itinerary not found: " + id));
        return mapper.toResponse(itinerary);
    }

    // ================= UPDATE =================
    @Override
    public AdminItineraryResponse updateItinerary(Long id, AdminItineraryCreateRequest request) {
        AdminItinerary existingItinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin itinerary not found: " + id));

        // 1. Fetch destinations
        List<Long> destIds = request.getItems().stream()
                .map(AdminItineraryItemRequest::getDestinationId).distinct().toList();
        var destinations = destinationRepository.findAllById(destIds);

        // 2. Map the request to a fresh temporary entity
        AdminItinerary updatedData = mapper.toEntity(request, destinations);

        // 3. Update existing entity fields
        existingItinerary.setTitle(updatedData.getTitle());
        existingItinerary.setDescription(updatedData.getDescription());
        existingItinerary.setTheme(updatedData.getTheme());
        existingItinerary.setTotalDays(updatedData.getTotalDays());
        existingItinerary.setActive(updatedData.getActive());
        existingItinerary.setEstimatedBudget(updatedData.getEstimatedBudget());

        // 4. Update items
        existingItinerary.getItems().clear();
        for (AdminItineraryItem newItem : updatedData.getItems()) {
            existingItinerary.addItem(newItem);
        }

        return mapper.toResponse(itineraryRepository.save(existingItinerary));
    }

    // ================= SOFT DELETE =================
    @Override
    public void softDeleteItinerary(Long id) {
        AdminItinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin itinerary not found: " + id));
        itinerary.setActive(false);
        itineraryRepository.save(itinerary);
    }

    // ================= UPDATE ITEM =================
    @Override
    public AdminItineraryItemResponse updateItem(Long itemId, AdminItineraryItemUpdateRequest request) {
        AdminItineraryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Itinerary item not found: " + itemId));

        item.setTitle(request.getTitle());
        item.setNotes(request.getDescription());
        item.setDayNumber(request.getDayNumber());
        item.setOrderInDay(request.getOrderInDay());
        item.setStartTime(request.getStartTime());
        item.setEndTime(request.getEndTime());
        item.setDurationMinutes(request.getDurationMinutes());
        item.setActivityType(request.getActivityType());

        itemRepository.save(item);
        return mapper.toItemResponse(item);
    }

    @Override
    public AdminItineraryItemResponse deleteItem(Long itemId) {
        AdminItineraryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Itinerary item not found: " + itemId));
        AdminItinerary itinerary = item.getAdminItinerary();
        itinerary.getItems().remove(item);
        itemRepository.delete(item);
        return mapper.toItemResponse(item);
    }

}
