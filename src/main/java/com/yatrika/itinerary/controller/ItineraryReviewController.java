package com.yatrika.itinerary.controller;

import com.yatrika.itinerary.dto.request.ItineraryReviewRequest;
import com.yatrika.itinerary.dto.response.ItineraryReviewResponse;
import com.yatrika.itinerary.service.ItineraryReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/itineraries/{itineraryId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Itinerary Reviews", description = "Ratings and feedback for shared trips")
public class ItineraryReviewController {

    private final ItineraryReviewService reviewService;

    @PostMapping
    public ResponseEntity<ItineraryReviewResponse> postReview(
            @PathVariable Long itineraryId,
            @RequestBody ItineraryReviewRequest request) {

        // In a real app, get these from SecurityContext (JWT)
        Long currentUserId = 1L;
        String currentUserName = "TravelerJohn";

        return ResponseEntity.ok(reviewService.addReview(itineraryId, request, currentUserId, currentUserName));
    }
}