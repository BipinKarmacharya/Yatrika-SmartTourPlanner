package com.yatrika.itinerary.repository;

import com.yatrika.itinerary.domain.UserLikedItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserLikedItineraryRepository extends JpaRepository<UserLikedItinerary, Long> {
    boolean existsByItineraryIdAndUserId(Long itineraryId, Long userId);
    Optional<UserLikedItinerary> findByItineraryIdAndUserId(Long itineraryId, Long userId);
    void deleteByItineraryIdAndUserId(Long itineraryId, Long userId);
    // This method counts how many users liked this itinerary
    @Query("SELECT COUNT(uli) FROM UserLikedItinerary uli WHERE uli.itinerary.id = :itineraryId")
    Long countByItineraryId(@Param("itineraryId") Long itineraryId);
}
