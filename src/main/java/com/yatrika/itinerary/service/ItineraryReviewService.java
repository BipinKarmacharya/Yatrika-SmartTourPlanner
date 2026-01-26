package com.yatrika.itinerary.service;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.dto.request.ItineraryReviewRequest;
import com.yatrika.itinerary.dto.response.ItineraryReviewResponse;

public interface ItineraryReviewService {
    public ItineraryReviewResponse addReview(Long itineraryId, ItineraryReviewRequest request, Long userId, String userName);
}
