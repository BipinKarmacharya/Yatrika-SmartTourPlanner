package com.yatrika.itinerary.service;

import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface ItineraryService {

    // ==================== DISCOVERY ====================
    List<ItineraryResponse> getAdminTemplates();

    Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable);

    Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable);

    ItineraryResponse getItineraryById(Long id, Long currentUserId);

    // ==================== PERSONAL MANAGEMENT ====================
    ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId);

    ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId);

    Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable);

    ItineraryResponse completeTrip(Long itineraryId, Long userId);

    ItineraryResponse uploadItineraryImages(Long itineraryId, List<MultipartFile> files, Long userId);

    void deleteItinerary(Long id, Long userId);

    void deleteAllMyItineraries(Long userId);

    // ==================== COPY & SHARE ====================
    ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId, LocalDate startDate);

    ItineraryResponse shareTrip(Long itineraryId, Long userId);

    ItineraryResponse unshareTrip(Long itineraryId, Long userId);

    // ==================== ITEM MANAGEMENT ====================
    ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest request, Long userId);

    ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId);

    void removeItem(Long itineraryId, Long itemId, Long userId);

    void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId);

    void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId);

    // ==================== ADMIN TEMPLATES ====================
    ItineraryResponse createAdminTemplate(ItineraryRequest request);

    ItineraryResponse createAdminTemplate(ItineraryRequest request, List<MultipartFile> files);

    ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request);

    void removeAdminTemplate(Long templateId);

    ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest request);

    ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest request);

    void removeItemFromTemplate(Long templateId, Long itemId);

    ItineraryResponse uploadAdminTemplateImages(Long templateId, List<MultipartFile> files);

    ItineraryResponse removeAdminTemplateImage(Long templateId, Long imageId);

    ItineraryResponse setAdminTemplateCoverImage(Long templateId, Long imageId);

    // ==================== SOCIAL (SAVE / LIKE) ====================
    ItineraryResponse saveItinerary(Long itineraryId, Long userId);

    ItineraryResponse unsaveItinerary(Long itineraryId, Long userId);

    List<ItineraryResponse> getMySavedItineraries(Long userId);

    boolean isItinerarySaved(Long itineraryId, Long userId);

    ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId);
}







//package com.yatrika.itinerary.service;
//
//import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
//import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
//import com.yatrika.itinerary.dto.request.ItineraryRequest;
//import com.yatrika.itinerary.dto.response.ItineraryResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDate;
//import java.util.List;
//
//public interface ItineraryService {
//
//    // -------------------- DISCOVERY --------------------
//    List<ItineraryResponse> getAdminTemplates();
//
//    Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable);
//
//    Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable);
//
//    ItineraryResponse getItineraryById(Long id, Long currentUserId);
//
//    // -------------------- PERSONAL MANAGEMENT --------------------
//    ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId);
//
//    ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId);
//
////    ItineraryResponse updateFullItinerary(Long id, ItineraryRequest request, Long userId);
//
//    Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable);
//
//    ItineraryResponse completeTrip(Long itineraryId, Long userId);
//
//    ItineraryResponse uploadItineraryImages(Long itineraryId, List<MultipartFile> files, Long userId);
//
//    void deleteItinerary(Long id, Long userId);
//
//    void deleteAllMyItineraries(Long userId);
//
//    // -------------------- COPY & SHARE --------------------
//    ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId, LocalDate startDate);
//
//    ItineraryResponse shareTrip(Long itineraryId, Long currentUserId);
//
//    ItineraryResponse unshareTrip(Long id, Long userId);
//
//    // -------------------- ITEM MANAGEMENT --------------------
//    ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest itemRequest, Long userId);
//
//    ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId);
//
//    void removeItem(Long itineraryId, Long itemId, Long userId);
//
//    void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId);
//
//    void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId);
//
//    // -------------------- ADMIN SPECIFIC --------------------
//    ItineraryResponse createAdminTemplate(ItineraryRequest request);
//
//    ItineraryResponse createAdminTemplate(ItineraryRequest request, List<MultipartFile> files);
//
//    ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request);
//
//    void removeAdminTemplate(Long templateId);
//
//    ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest itemRequest);
//
//    ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest itemRequest);
//
//    void removeItemFromTemplate(Long templateId, Long itemId);
//
//    ItineraryResponse removeAdminTemplateImage(Long templateId, Long imageId);
//
//    ItineraryResponse setAdminTemplateCoverImage(Long templateId, Long imageId);
//
//
////    ItineraryResponse updateFullAdminTemplate(Long templateId, ItineraryRequest request);
//
//    ItineraryResponse uploadAdminTemplateImages(Long templateId, List<MultipartFile> files);
//
//    // -------------------- Save and Unsaved -----------------
//    ItineraryResponse saveItinerary(Long itineraryId, Long userId);
//
//    ItineraryResponse unsaveItinerary(Long itineraryId, Long userId);
//
//    List<ItineraryResponse> getMySavedItineraries(Long userId);
//
//    boolean isItinerarySaved(Long itineraryId, Long userId);
//
//    // -------------------- Like Toggle ------------------
//    ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId); // Changed from like/unlike to toggle
//}