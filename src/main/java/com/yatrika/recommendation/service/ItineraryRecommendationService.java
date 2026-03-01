package com.yatrika.recommendation.service;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.recommendation.dto.ItineraryRecommendationResponse;
import com.yatrika.recommendation.mapper.ItineraryRecommendationMapper;
import com.yatrika.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ItineraryRecommendationService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryRecommendationMapper mapper;

    public List<ItineraryRecommendationResponse> recommendForUser(User user) {
        // 1. Normalize interests to uppercase strings
        List<String> interestCodes = user.getUserInterests()
                .stream()
                .map(ui -> ui.getInterest().getCode().toUpperCase())
                .toList();

        List<Itinerary> recommendedList;
        Pageable topTen = PageRequest.of(0, 10);

        if (interestCodes.isEmpty()) {
            // .getContent() works here because findTopPublicItinerariesByLikes returns Page
            recommendedList = new ArrayList<>(itineraryRepository.findTopPublicItinerariesByLikes(topTen).getContent());
        } else {
            // Step A: Get specific matches.
            // No .getContent() here because findRecommended already returns a List!
            recommendedList = new ArrayList<>(itineraryRepository.findRecommended(interestCodes, topTen));

            // Step B: Fallback if matching pool is too small (under 5 results)
            if (recommendedList.size() < 5) {
                List<Itinerary> popular = itineraryRepository.findTopPublicItinerariesByLikes(topTen).getContent();
                for (Itinerary p : popular) {
                    // Only add if not already in the list to avoid duplicates
                    if (recommendedList.stream().noneMatch(r -> r.getId().equals(p.getId()))
                            && recommendedList.size() < 10) {
                        recommendedList.add(p);
                    }
                }
            }
        }

        return mapper.toResponse(recommendedList);
    }

    public List<ItineraryRecommendationResponse> getGeneralRecommendations() {
        Pageable topTen = PageRequest.of(0, 10);
        // Directly fetch the most liked itineraries for guests
        List<Itinerary> popular = itineraryRepository.findTopPublicItinerariesByLikes(topTen).getContent();
        return mapper.toResponse(popular);
    }
}
