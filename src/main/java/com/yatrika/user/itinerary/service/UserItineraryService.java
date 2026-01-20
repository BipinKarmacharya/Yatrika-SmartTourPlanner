package com.yatrika.user.itinerary.service;

import com.yatrika.user.itinerary.dto.request.UserItineraryCreateRequest;
import com.yatrika.user.itinerary.dto.response.UserItineraryResponse;

import java.util.List;

public interface UserItineraryService {

    // ================= TRIP CRUD =================
    UserItineraryResponse createItinerary(UserItineraryCreateRequest request);

    UserItineraryResponse getItineraryById(Long id);

    List<UserItineraryResponse> getAllUserItineraries(Long userId);

    UserItineraryResponse updateItinerary(Long id, UserItineraryCreateRequest request);

    void softDeleteItinerary(Long id);

    // ================= ITEM CRUD =================
    UserItineraryResponse updateItineraryItem(Long itemId, UserItineraryCreateRequest itemRequest);

    UserItineraryResponse deleteItineraryItem(Long itemId);

    // ================= Copy Admin Itinerary =================
    UserItineraryResponse copyAdminItinerary(Long adminItineraryId, Long userId);
}
