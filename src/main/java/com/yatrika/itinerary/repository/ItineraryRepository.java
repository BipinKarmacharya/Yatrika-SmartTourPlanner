package com.yatrika.itinerary.repository;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long>, JpaSpecificationExecutor<Itinerary> {

    // TAB 1: Admin-Created Itineraries (Curated)
    // We look for status TEMPLATE and the Admin flag
    List<Itinerary> findByStatusAndIsAdminCreatedTrue(ItineraryStatus status);

    // TAB 2: Public Shared Itineraries (Community)
    // Must be COMPLETED and marked as PUBLIC
    // We exclude Admin-created ones so they don't mix
    Page<Itinerary> findByStatusAndIsPublicTrueAndIsAdminCreatedFalse(ItineraryStatus status, Pageable pageable);

    // USER VIEW: Get all itineraries belonging to a specific user
    List<Itinerary> findByUserIdAndStatusNot(Long userId, ItineraryStatus status);

    // VALIDATION: Check if a user already has a trip with a specific sourceId
    // (Prevents double-copying the same template)
    boolean existsByUserIdAndSourceId(Long userId, Long sourceId);

    // Get all trips owned by the user, ordered by creation date
    Page<Itinerary> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT i FROM Itinerary i " +
            "LEFT JOIN FETCH i.items items " +
            "LEFT JOIN FETCH items.destination " + // Fetch destinations in the same query
            "WHERE i.id = :id")
    Optional<Itinerary> findByIdWithDetails(@Param("id") Long id);
}