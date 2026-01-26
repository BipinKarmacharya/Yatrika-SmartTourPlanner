package com.yatrika.itinerary.service.impl;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryReview;
import com.yatrika.itinerary.dto.request.ItineraryReviewRequest;
import com.yatrika.itinerary.dto.response.ItineraryReviewResponse;
import com.yatrika.itinerary.mapper.ItineraryReviewMapper; // Use a specific review mapper
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.itinerary.repository.ItineraryReviewRepository;
import com.yatrika.itinerary.service.ItineraryReviewService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ItineraryReviewServiceImpl implements ItineraryReviewService { // Changed to 'implements'

    private final ItineraryRepository itineraryRepository;
    private final ItineraryReviewRepository reviewRepository;
    private final ItineraryReviewMapper reviewMapper; // Ensure you have a ReviewMapper

    @Override
    @Transactional
    public ItineraryReviewResponse addReview(Long itineraryId, ItineraryReviewRequest request, Long userId, String userName) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        ItineraryReview review = ItineraryReview.builder()
                .itinerary(itinerary)
                .userId(userId)
                .userName(userName)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        // Call the internal helper
        updateItineraryRating(itinerary);

        return reviewMapper.toResponse(review);
    }

    // This stays here as a private helper
    private void updateItineraryRating(Itinerary itinerary) {
        Double avg = reviewRepository.calculateAverageRating(itinerary.getId());
        itinerary.setAverageRating(avg != null ? avg : 0.0);
        itineraryRepository.save(itinerary);
    }
}