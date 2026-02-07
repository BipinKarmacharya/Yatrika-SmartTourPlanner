package com.yatrika.itinerary.service;

import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItineraryService {

    // -------------------- DISCOVERY --------------------
    List<ItineraryResponse> getAdminTemplates();
    Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable);
    Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable);
    ItineraryResponse getItineraryById(Long id, Long currentUserId);

    // -------------------- PERSONAL MANAGEMENT --------------------
    ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId);
    ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId);
    ItineraryResponse updateFullItinerary(Long id, ItineraryRequest request, Long userId);
    Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable);
    ItineraryResponse completeTrip(Long itineraryId, Long userId);

    // -------------------- COPY & SHARE --------------------
    ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId);
    ItineraryResponse shareTrip(Long itineraryId, Long currentUserId);
    ItineraryResponse unshareTrip(Long id, Long userId);

    // -------------------- ITEM MANAGEMENT --------------------
    ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest itemRequest, Long userId);
    ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId);
    void removeItem(Long itineraryId, Long itemId, Long userId);
    void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId);
    void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId);

    // -------------------- ADMIN SPECIFIC --------------------
    ItineraryResponse createAdminTemplate(ItineraryRequest request);
    ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request);
    void removeAdminTemplate(Long templateId);
    ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest itemRequest);
    ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest itemRequest);
    void removeItemFromTemplate(Long templateId, Long itemId);
    ItineraryResponse updateFullAdminTemplate(Long templateId, ItineraryRequest request);

    // -------------------- Save and Unsaved -----------------
    ItineraryResponse saveItinerary(Long itineraryId, Long userId);
    ItineraryResponse unsaveItinerary(Long itineraryId, Long userId);
    List<ItineraryResponse> getMySavedItineraries(Long userId);
    boolean isItinerarySaved(Long itineraryId, Long userId);

    // -------------------- Like Toggle ------------------
    ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId); // Changed from like/unlike to toggle
}