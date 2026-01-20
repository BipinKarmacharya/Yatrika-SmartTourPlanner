package com.yatrika.user.itinerary.repository;

import com.yatrika.user.itinerary.domain.UserItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserItineraryRepository extends JpaRepository<UserItinerary, Long> {

    // Find all active itineraries of a user
    List<UserItinerary> findByUserIdAndActiveTrue(Long userId);
}
