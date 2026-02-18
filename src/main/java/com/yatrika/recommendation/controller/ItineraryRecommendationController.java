package com.yatrika.recommendation.controller;

import com.yatrika.recommendation.dto.ItineraryRecommendationResponse;
import com.yatrika.recommendation.service.ItineraryRecommendationService;
import com.yatrika.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class ItineraryRecommendationController {

    private final ItineraryRecommendationService recommendationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/itineraries")
    public List<ItineraryRecommendationResponse> getRecommendations(@RequestParam(defaultValue = "10") int limit) {
        var user = currentUserService.getCurrentUserEntity();
        return recommendationService.recommendForUser(user);
    }
}
