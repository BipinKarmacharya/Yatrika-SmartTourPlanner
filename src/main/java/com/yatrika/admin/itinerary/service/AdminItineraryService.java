package com.yatrika.admin.itinerary.service;

import com.yatrika.admin.itinerary.dto.request.AdminItineraryCreateRequest;
import com.yatrika.admin.itinerary.dto.request.AdminItineraryItemUpdateRequest;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryItemResponse;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryResponse;

import java.util.List;

public interface AdminItineraryService {

    AdminItineraryResponse createItinerary(AdminItineraryCreateRequest request);

    List<AdminItineraryResponse> getAllActiveItineraries();

    AdminItineraryResponse getItineraryById(Long id);

    AdminItineraryResponse updateItinerary(Long id, AdminItineraryCreateRequest request);

    void softDeleteItinerary(Long id);

    AdminItineraryItemResponse updateItem(Long itemId, AdminItineraryItemUpdateRequest request);

    AdminItineraryItemResponse deleteItem(Long itemId);
}
