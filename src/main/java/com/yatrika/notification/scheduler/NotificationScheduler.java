package com.yatrika.notification.scheduler;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.notification.domain.Notification;
import com.yatrika.notification.dto.response.WeatherResponse;
import com.yatrika.notification.repository.NotificationPreferenceRepository;
import com.yatrika.notification.service.NotificationService;
import com.yatrika.notification.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final ItineraryRepository itineraryRepository;
    private final NotificationService notificationService;
    private final WeatherService weatherService;
    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Reminder for trips starting tomorrow.
     * Checks daily at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendTripReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Itinerary> upcomingTrips = itineraryRepository.findAllByStartDate(tomorrow);

        for (Itinerary trip : upcomingTrips) {
            if (trip.getUser() != null) {
                // CHECK PREFERENCE
                boolean wantsReminder = preferenceRepository.findById(trip.getUser().getId())
                        .map(p -> p.isTripReminders())
                        .orElse(true); // Default to true

                if (wantsReminder) {
                    notificationService.createAndSend(
                            trip.getUser(),
                            "Trip Starts Tomorrow!",
                            "Pack your bags! Your journey '" + trip.getTitle() + "' begins tomorrow.",
                            Notification.NotificationType.TRIP_REMINDER,
                            trip.getId()
                    );
                }
            }
        }
    }

    /**
     * Daily Weather Alert.
     * Checks daily at 7:00 AM for trips happening today.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void checkWeatherForActiveTrips() {
        LocalDate today = LocalDate.now();
        List<Itinerary> activeTrips = itineraryRepository.findAllActiveItineraries(today);

        for (Itinerary trip : activeTrips) {
            // PREFERENCE CHECK
            boolean wantsWeather = preferenceRepository.findById(trip.getUser().getId())
                    .map(p -> p.isWeatherAlerts())
                    .orElse(true);

            if (!wantsWeather) continue;

            // Determine which "Day #" of the trip this is
            long dayNumber = ChronoUnit.DAYS.between(trip.getStartDate(), today) + 1;

            // Find activity for today in the itinerary items
            trip.getItems().stream()
                    .filter(item -> item.getDayNumber().equals((int) dayNumber))
                    .findFirst()
                    .ifPresent(item -> {
                        if (item.getDestination() != null) {
                            processWeatherAlert(trip, item);
                        }
                    });
        }
    }

    private void processWeatherAlert(Itinerary trip, ItineraryItem item) {
        try {
            WeatherResponse weather = weatherService.fetchWeather(
                    item.getDestination().getLatitude(),
                    item.getDestination().getLongitude()
            );

            // Logic: If weather is Rain or Stormy
            String mainWeather = weather.getWeather().get(0).getMain();
            if (mainWeather.equalsIgnoreCase("Rain") || mainWeather.equalsIgnoreCase("Thunderstorm")) {
                String title = "Weather Alert: " + item.getDestination().getName();
                String message = "It looks like " + weather.getWeather().get(0).getDescription() +
                        " is expected today. Bring an umbrella for your visit!";

                notificationService.createAndSend(
                        trip.getUser(),
                        title,
                        message,
                        Notification.NotificationType.WEATHER_ALERT,
                        trip.getId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to fetch weather for itinerary {}: {}", trip.getId(), e.getMessage());
        }
    }
}