package com.yatrika.itinerary.repository;

import com.yatrika.itinerary.domain.UserLikedItinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLikedItineraryRepository extends JpaRepository<UserLikedItinerary, Long> {
    boolean existsByItineraryIdAndUserId(Long itineraryId, Long userId);
    Optional<UserLikedItinerary> findByItineraryIdAndUserId(Long itineraryId, Long userId);
    void deleteByItineraryIdAndUserId(Long itineraryId, Long userId);
}
