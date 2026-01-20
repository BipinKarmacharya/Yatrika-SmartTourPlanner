package com.yatrika.admin.itinerary.repository;

import com.yatrika.admin.itinerary.domain.AdminItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminItineraryRepository extends JpaRepository<AdminItinerary, Long> {

    /**
     * Used for user-facing itinerary browsing
     */
    List<AdminItinerary> findByActiveTrue();

    @Query("""
        SELECT DISTINCT ai
        FROM AdminItinerary ai
        JOIN ai.items item
        WHERE ai.active = true
          AND item.destination.id = :destinationId
    """)
    List<AdminItinerary> findActiveByDestinationId(@Param("destinationId") Long destinationId);


    /**
     * Admin dashboard / filtering
     */
    List<AdminItinerary> findByThemeIgnoreCase(String theme);
}
