package com.yatrika.itinerary.service;

import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ItineraryService {

    // Core Creation
    ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId);

    // Discovery Tabs
    List<ItineraryResponse> getAdminTemplates();
    Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable);
    ItineraryResponse getItineraryById(Long id);

    // Personal Management
    Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable);

    // Copy & Share Logic
    ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId);
    ItineraryResponse shareTrip(Long itineraryId, Long currentUserId);

    // --- ITEM MANAGEMENT (The new methods) ---

    ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest itemRequest, Long userId);

    ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId);

    void removeItem(Long itineraryId, Long itemId, Long userId);

    ItineraryResponse createAdminTemplate(ItineraryRequest request);
    ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest itemRequest);
    Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable);

    ItineraryResponse completeTrip(Long itineraryId, Long userId);
}