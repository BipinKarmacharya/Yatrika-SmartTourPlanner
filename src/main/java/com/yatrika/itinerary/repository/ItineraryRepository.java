package com.yatrika.itinerary.repository;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    // User-specific queries
    Page<Itinerary> findByUserId(Long userId, Pageable pageable);

    List<Itinerary> findByUserIdAndStatus(Long userId, ItineraryStatus status);

    @Query("SELECT i FROM Itinerary i WHERE i.user.id = :userId AND " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:startDate IS NULL OR i.startDate >= :startDate) AND " +
            "(:endDate IS NULL OR i.endDate <= :endDate)")
    Page<Itinerary> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") ItineraryStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // Public itineraries (for inspiration)
    Page<Itinerary> findByIsPublicTrueAndStatus(ItineraryStatus status, Pageable pageable);

    Page<Itinerary> findByIsPublicTrueAndStatusAndUserIdNot(
            ItineraryStatus status, Long excludedUserId, Pageable pageable);

    // Count queries
    Long countByUserId(Long userId);

    Long countByUserIdAndStatus(Long userId, ItineraryStatus status);

    // Check ownership
    boolean existsByIdAndUserId(Long itineraryId, Long userId);
}