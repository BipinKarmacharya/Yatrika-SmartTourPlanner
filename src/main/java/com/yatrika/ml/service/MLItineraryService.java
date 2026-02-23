package com.yatrika.ml.service;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.itinerary.domain.ActivityType;
import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.domain.ItineraryStatus;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.ml.dto.request.MLPredictRequest;
import com.yatrika.ml.dto.response.DailyPlan;
import com.yatrika.ml.dto.response.MLPredictResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MLItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final DestinationRepository destinationRepository;
    private final RestClient restClient;

    /**
     * Call FastAPI ML service to get predicted itinerary.
     */
//    public MLPredictResponse getPredictionFromFastAPI(MLPredictRequest request) {
//        try {
//            return restClient.post()
//                    .uri("/predict")
//                    .body(request)
//                    .retrieve()
//                    .body(MLPredictResponse.class);
//        } catch (RestClientException e) {
//            throw new RuntimeException("ML service unavailable", e);
//        }
//    }
    public MLPredictResponse getPredictionFromFastAPI(MLPredictRequest request) {
        try {
            // We keep it as a List because FastAPI/Pydantic is strictly validating the type
            // But we ensure every item is lowercase to match the ML model training data
            List<String> normalizedInterests = request.getInterests().stream()
                    .map(String::toLowerCase)
                    .toList();

            // Create the body map
            Map<String, Object> fastApiBody = new HashMap<>();
            fastApiBody.put("city", request.getCity());
            fastApiBody.put("budget", request.getBudget().toString());
            fastApiBody.put("days", request.getDays());
            fastApiBody.put("interests", normalizedInterests); // Send as List, not String

            log.info("Sending request to FastAPI: {}", fastApiBody);

            return restClient.post()
                    .uri("/predict")
                    .body(fastApiBody)
                    .retrieve()
                    .body(MLPredictResponse.class);
        } catch (RestClientException e) {
            log.error("FastAPI Call Failed: {}", e.getMessage());
            throw new RuntimeException("ML service unavailable", e);
        }
    }

    /**
     * Save ML-generated itinerary to DB.
     */
    @Transactional
    public Itinerary saveMLPlan(
            String city,
            int days,
            LocalDate startDate,
            List<String> interests,
            Map<String, List<String>> itineraryData,
            Long userId
    ) {
        // 1. Title and Date Logic
        String finalCity = (city != null) ? city : "AI Plan";
        LocalDate start = (startDate != null) ? startDate : LocalDate.now();
        LocalDate end = start.plusDays(days > 0 ? days - 1 : 0);

        Itinerary itinerary = Itinerary.builder()
                .title("AI Plan: " + finalCity)
                .description("Smart Trip generated for " + finalCity)
                .theme((interests != null && !interests.isEmpty()) ? interests.get(0) : "General")
                .userId(userId)
                .startDate(start)
                .endDate(end)
                .totalDays(days)
                .status(ItineraryStatus.DRAFT)
                .isAdminCreated(false)
                .isPublic(false)
                .items(new ArrayList<>())
                .build();

        // 2. Fix the "Only Day 1" issue
        if (itineraryData != null) {
            itineraryData.forEach((dayKey, places) -> {
                try {
                    // Parse the String key "1" back to Integer 1
                    int dayNumber = Integer.parseInt(dayKey);

                    for (int i = 0; i < places.size(); i++) {
                        String placeName = places.get(i);
                        ItineraryItem item = ItineraryItem.builder()
                                .itinerary(itinerary)
                                .title(placeName)
                                .dayNumber(dayNumber)
                                .orderInDay(i + 1)
                                .activityType(ActivityType.VISIT)
                                .startTime(calculateDefaultTime(i))
                                .isVisited(false)
                                .build();
                        itinerary.addItem(item);
                    }
                } catch (NumberFormatException e) {
                    log.error("Failed to parse day number from key: {}", dayKey);
                }
            });
        }

        return itineraryRepository.save(itinerary);
    }

    /**
     * Stagger activities every 2 hours starting from 9 AM.
     */
    private LocalTime calculateDefaultTime(int index) {
        int hour = 9 + (index * 2);
        if (hour > 20) hour = 20;
        return LocalTime.of(hour, 0);
    }
}


//package com.yatrika.ml.service;
//
//import com.yatrika.destination.domain.Destination;
//import com.yatrika.destination.repository.DestinationRepository;
//import com.yatrika.itinerary.domain.Itinerary;
//import com.yatrika.itinerary.domain.ItineraryItem;
//import com.yatrika.itinerary.domain.ItineraryStatus;
//import com.yatrika.itinerary.repository.ItineraryRepository;
//import com.yatrika.ml.dto.request.MLPredictRequest; // Create this to match your FastAPI input
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.client.RestClient;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class MLItineraryService {
//
//    private final ItineraryRepository itineraryRepository;
//    private final DestinationRepository destinationRepository;
//
//    // In production, move this URL to application.properties
//    private final RestClient restClient = RestClient.builder().baseUrl("http://127.0.0.1:8000").build();
//
//    public Map<String, List<String>> getPredictionFromFastAPI(MLPredictRequest request) {
//        return restClient.post()
//                .uri("/predict")
//                .body(request)
//                .retrieve()
//                .body(new ParameterizedTypeReference<Map<String, List<String>>>() {});
//    }
//
//    // Inside MLItineraryService.java
//
//    @Transactional
//    public Itinerary saveMLPlan(MLPredictRequest metadata, Map<String, List<String>> itineraryData, Long userId, LocalDate startDate) {
//
//        LocalDate effectiveStart = (startDate != null) ? startDate : LocalDate.now();
//        LocalDate effectiveEnd = effectiveStart.plusDays(metadata.getDays() - 1);
//
//        Itinerary itinerary = Itinerary.builder()
//                .title("AI Plan: " + metadata.getCity())
//                .description("Smart Trip generated for " + metadata.getCity())
//                .theme(metadata.getInterests().isEmpty() ? "General" : metadata.getInterests().get(0))
//                .userId(userId)
//                .startDate(effectiveStart)
//                .endDate(effectiveEnd)
//                .totalDays(metadata.getDays())
//                .status(ItineraryStatus.DRAFT)
//                .isAdminCreated(false)
//                .isPublic(false)
//                .items(new ArrayList<>())
//                .build();
//
//        // Map the incoming itineraryData to ItineraryItems
//        itineraryData.forEach((dayKey, placeNames) -> {
//            int dayNumber = Integer.parseInt(dayKey.replaceAll("[^0-9]", ""));
//
//            for (int i = 0; i < placeNames.size(); i++) {
//                String name = placeNames.get(i);
//                var dest = destinationRepository.findByNameIgnoreCase(name).orElse(null);
//
//                ItineraryItem item = ItineraryItem.builder()
//                        .itinerary(itinerary)
//                        .destination(dest)
//                        .title(name)
//                        .dayNumber(dayNumber)
//                        .orderInDay(i + 1)
//                        .activityType(dest != null ? "VISIT" : "GENERAL") // Mark as general if no DB match
//                        .startTime(calculateDefaultTime(i))
//                        .isVisited(false)
//                        .build();
//                itinerary.addItem(item);
//            }
//        });
//
//        return itineraryRepository.save(itinerary);
//    }
//
//    private LocalTime calculateDefaultTime(int index) {
//        // Stagger activities every 2 hours starting from 09:00 AM
//        int hour = 9 + (index * 2);
//        if (hour > 20) hour = 20; // Cap it at 8:00 PM
//        return LocalTime.of(hour, 0);
//    }
//}