package com.yatrika.ml.service;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.domain.ItineraryStatus;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.ml.dto.request.MLPredictRequest; // Create this to match your FastAPI input
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MLItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final DestinationRepository destinationRepository;

    // In production, move this URL to application.properties
    private final RestClient restClient = RestClient.builder().baseUrl("http://127.0.0.1:8000").build();

    public Map<String, List<String>> getPredictionFromFastAPI(MLPredictRequest request) {
        return restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, List<String>>>() {});
    }

    // Inside MLItineraryService.java

    @Transactional
    public Itinerary saveMLPlan(MLPredictRequest metadata, Map<String, List<String>> itineraryData, Long userId) {
        Itinerary itinerary = Itinerary.builder()
                .title("AI Plan: " + metadata.getCity())
                .description("Smart Trip generated for " + metadata.getCity())
                // Using the first interest as the theme
                .theme(metadata.getInterests().isEmpty() ? "General" : metadata.getInterests().get(0))
                .userId(userId)
                .status(ItineraryStatus.DRAFT)
                .totalDays(metadata.getDays())
                .isAdminCreated(false)
                .isPublic(false)
                .items(new ArrayList<>())
                .build();

        // Map the incoming itineraryData to ItineraryItems
        itineraryData.forEach((dayKey, placeNames) -> {
            // Extract digits (e.g., "Day 1" -> 1)
            int dayNumber = Integer.parseInt(dayKey.replaceAll("[^0-9]", ""));

            for (int i = 0; i < placeNames.size(); i++) {
                String name = placeNames.get(i);
                var dest = destinationRepository.findByNameIgnoreCase(name).orElse(null);

                ItineraryItem item = ItineraryItem.builder()
                        .itinerary(itinerary)
                        .destination(dest)
                        .title(name)
                        .dayNumber(dayNumber)
                        .orderInDay(i + 1)
                        .activityType("VISIT")
                        .isVisited(false)
                        .build();
                itinerary.addItem(item);
            }
        });

        return itineraryRepository.save(itinerary);
    }
}