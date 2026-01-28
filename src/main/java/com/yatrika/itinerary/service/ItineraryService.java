package com.yatrika.itinerary.service;

import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ItineraryService {

    // --- CORE CREATION & DISCOVERY ---
    ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId);
    List<ItineraryResponse> getAdminTemplates();
    Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable);
    ItineraryResponse getItineraryById(Long id);
    Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable);

    // --- PERSONAL MANAGEMENT ---
    Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable);
    ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId); // Added
    ItineraryResponse completeTrip(Long itineraryId, Long userId);

    // --- COPY & SHARE LOGIC ---
    ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId);
    ItineraryResponse shareTrip(Long itineraryId, Long currentUserId);

    // --- ITEM & ACTIVITY MANAGEMENT ---
    ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest itemRequest, Long userId);
    ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId);
    void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId); // Added
    void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId); // Added
    void removeItem(Long itineraryId, Long itemId, Long userId);

    // --- ADMIN SPECIFIC ---
    ItineraryResponse createAdminTemplate(ItineraryRequest request);
    ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest itemRequest);
}