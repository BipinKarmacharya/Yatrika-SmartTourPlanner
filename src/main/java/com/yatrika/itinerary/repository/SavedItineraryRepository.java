package com.yatrika.itinerary.repository;

import com.yatrika.itinerary.domain.SavedItinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedItineraryRepository extends JpaRepository<SavedItinerary, Long> {
    boolean existsByItineraryIdAndUserId(Long itineraryId, Long userId);
    Optional<SavedItinerary> findByItineraryIdAndUserId(Long itineraryId, Long userId);
    List<SavedItinerary> findByUserId(Long userId);
    void deleteByItineraryIdAndUserId(Long itineraryId, Long userId);
}


