package com.yatrika.itinerary.service;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.repository.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MigrationService {

    private final ItineraryRepository itineraryRepository;
    private final DestinationRepository destinationRepository;

    @Transactional
    public void migrateItineraryData() {
        List<Itinerary> itineraries = itineraryRepository.findAll();

        // Create a new list for modified itineraries
        List<Itinerary> updatedItineraries = new ArrayList<>();

        for (Itinerary itinerary : itineraries) {
            // Fix 1: Ensure tags collection is mutable (Itinerary.tags is List<String>)
            if (itinerary.getTags() != null) {
                // Create a new mutable ArrayList from existing tags
                List<String> mutableTags = new ArrayList<>(itinerary.getTags());
                itinerary.setTags(mutableTags);
            } else {
                itinerary.setTags(new ArrayList<>());
            }

            // Fix 2: Ensure destinations list is mutable
            if (itinerary.getDestinations() != null) {
                // Create a new mutable ArrayList from existing destinations
                List<Destination> mutableDestinations = new ArrayList<>(itinerary.getDestinations());
                itinerary.setDestinations(mutableDestinations);
            } else {
                itinerary.setDestinations(new ArrayList<>());
            }

            // Fix 3: Fix each destination's tags (String[] -> String[])
            // Since tags is String[], we need to handle it differently
            for (Destination destination : itinerary.getDestinations()) {
                if (destination.getTags() != null) {
                    // For String array, we can either:
                    // 1. Convert to List<String> if you want to change the entity type
                    // 2. Keep as String[] but ensure it's a copy (not the original array)

                    // Option 1: Convert String[] to List<String> (if you plan to change entity)
                    // List<String> tagList = Arrays.asList(destination.getTags());
                    // destination.setTags(tagList.toArray(new String[0]));

                    // Option 2: Just make a copy of the array to avoid issues
                    String[] originalTags = destination.getTags();
                    String[] tagsCopy = Arrays.copyOf(originalTags, originalTags.length);
                    destination.setTags(tagsCopy);
                }
            }

            // Optional: Generate/update tags from theme
            updateTagsFromTheme(itinerary);

            // Optional: Calculate and update budget
            updateItineraryBudget(itinerary);

            updatedItineraries.add(itinerary);
        }

        // Save all updated itineraries
        itineraryRepository.saveAll(updatedItineraries);
    }

    private void updateTagsFromTheme(Itinerary itinerary) {
        if (itinerary.getTheme() != null && !itinerary.getTheme().trim().isEmpty()) {
            List<String> generatedTags = generateTagsFromTheme(itinerary.getTheme());

            // Add generated tags if not already present
            for (String tag : generatedTags) {
                if (!itinerary.getTags().contains(tag)) {
                    itinerary.getTags().add(tag);
                }
            }
        }
    }

    private void updateItineraryBudget(Itinerary itinerary) {
        if (itinerary.getEstimatedBudget() == null ||
                itinerary.getEstimatedBudget().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal calculatedBudget = calculateBudgetFromItems(itinerary);
            if (calculatedBudget.compareTo(BigDecimal.ZERO) > 0) {
                itinerary.setEstimatedBudget(calculatedBudget);
            }
        }
    }

    private List<String> generateTagsFromTheme(String theme) {
        if (theme == null || theme.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // For Java 8 compatibility
        String lowerTheme = theme.toLowerCase();
        if (lowerTheme.contains("adventure") || lowerTheme.contains("trekking")) {
            return new ArrayList<>(Arrays.asList("Adventure", "Hiking", "Mountains"));
        } else if (lowerTheme.contains("nature") && lowerTheme.contains("relaxation")) {
            return new ArrayList<>(Arrays.asList("Nature", "Relaxation", "Lakes"));
        } else if (lowerTheme.contains("culture") || lowerTheme.contains("history")) {
            return new ArrayList<>(Arrays.asList("Culture", "History", "Heritage"));
        } else if (lowerTheme.contains("spiritual") || lowerTheme.contains("peace")) {
            return new ArrayList<>(Arrays.asList("Spiritual", "Peaceful", "Meditation"));
        } else if (lowerTheme.contains("wildlife")) {
            return new ArrayList<>(Arrays.asList("Wildlife", "Nature", "Safari"));
        } else {
            return new ArrayList<>(Arrays.asList(theme));
        }
    }

    private BigDecimal calculateBudgetFromItems(Itinerary itinerary) {
        if (itinerary.getItems() == null || itinerary.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return itinerary.getItems().stream()
                .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}