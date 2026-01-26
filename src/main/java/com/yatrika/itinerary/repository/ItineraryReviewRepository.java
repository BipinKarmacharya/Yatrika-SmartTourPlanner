package com.yatrika.itinerary.repository;

import com.yatrika.itinerary.domain.ItineraryReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryReviewRepository extends JpaRepository<ItineraryReview, Long> {
    @Query("SELECT AVG(r.rating) FROM ItineraryReview r WHERE r.itinerary.id = :id")
    Double calculateAverageRating(@Param("id") Long id);

    List<ItineraryReview> findByItineraryIdOrderByCreatedAtDesc(Long itineraryId);
}
