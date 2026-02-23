package com.yatrika.notification.service;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.notification.scheduler.NotificationScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationTestService {

    private final NotificationScheduler scheduler;
    private final ItineraryRepository itineraryRepository;

    public void triggerManualWeatherCheck(Long itineraryId) {
        log.info("Manually triggering weather check for itinerary: {}", itineraryId);
        itineraryRepository.findById(itineraryId).ifPresent(itinerary -> {
            // We can make a public helper in the scheduler or move logic to a shared service
            // For now, let's just trigger the whole scheduler task for testing
            scheduler.checkWeatherForActiveTrips();
        });
    }
}