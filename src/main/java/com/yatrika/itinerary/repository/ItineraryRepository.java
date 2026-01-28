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

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long>, JpaSpecificationExecutor<Itinerary> {

    // --- DISCOVERY QUERIES ---

    // TAB 2: Curated Admin Templates
    List<Itinerary> findByStatusAndIsAdminCreatedTrue(ItineraryStatus status);

    // TAB 3: Community Shared Trips (Public & Completed)
    Page<Itinerary> findByStatusAndIsPublicTrueAndIsAdminCreatedFalse(ItineraryStatus status, Pageable pageable);


    // --- USER PERSONAL MANAGEMENT ---

    // Get all plans for "My Trips" tab, ordered by newest first
    Page<Itinerary> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Optimized detail fetch: Loads Itinerary + Items + Destination names in ONE database hit
    @Query("SELECT i FROM Itinerary i " +
            "LEFT JOIN FETCH i.items items " +
            "LEFT JOIN FETCH items.destination " +
            "WHERE i.id = :id")
    Optional<Itinerary> findByIdWithDetails(@Param("id") Long id);


    // --- SOCIAL & ANALYTICS ---

    // Count how many users have copied a specific original itinerary
    long countBySourceId(Long sourceId);

    // Check if user already has a copy of this specific template
    boolean existsByUserIdAndSourceId(Long userId, Long sourceId);


    // --- CLEANUP/MANAGEMENT ---

    // Find all active drafts for a user (useful for a "Resume Planning" widget)
    List<Itinerary> findByUserIdAndStatus(Long userId, ItineraryStatus status);
}