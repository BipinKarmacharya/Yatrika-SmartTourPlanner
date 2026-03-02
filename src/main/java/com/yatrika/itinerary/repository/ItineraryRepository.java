package com.yatrika.itinerary.repository;

import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long>, JpaSpecificationExecutor<Itinerary> {

    // --- DISCOVERY QUERIES ---
    // FETCH OPTIMIZATION: Use this in getItineraryById to load everything in 1 join
    @Query("SELECT i FROM Itinerary i " +
            "LEFT JOIN FETCH i.items items " +
            "LEFT JOIN FETCH items.destination " +
            "LEFT JOIN FETCH i.images " +
            "WHERE i.id = :id")
    Optional<Itinerary> findByIdWithDetails(@Param("id") Long id);

    // Used for Tab 2
    List<Itinerary> findByStatusAndIsAdminCreatedTrue(ItineraryStatus status);

    // Used for Tab 3
    @Query("""
            SELECT i FROM Itinerary i
            WHERE i.status = :status
              AND i.isPublic = true
              AND (i.isAdminCreated = false OR i.isAdminCreated IS NULL)
              AND i.sourceId IS NULL
            """)
    Page<Itinerary> findPublicCommunityTrips(@Param("status") ItineraryStatus status, Pageable pageable);


    List<Itinerary> findByUserId(Long userId);

    // --- USER PERSONAL MANAGEMENT ---

    // Get all plans for "My Trips" tab, ordered by newest first
    Page<Itinerary> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);


    // --- SOCIAL & ANALYTICS ---

    // Count how many users have copied a specific original itinerary
    long countBySourceId(Long sourceId);

    // Check if user already has a copy of this specific template
    boolean existsByUserIdAndSourceId(Long userId, Long sourceId);


    // --- CLEANUP/MANAGEMENT ---

    // Find all active drafts for a user (useful for a "Resume Planning" widget)
    List<Itinerary> findByUserIdAndStatus(Long userId, ItineraryStatus status);


    // Use DISTINCT to avoid duplicate itineraries if multiple tags match
    @Query("SELECT DISTINCT i FROM Itinerary i JOIN i.tags t " +
            "WHERE i.isPublic = true AND t IN :tags " +
            "ORDER BY i.likeCount DESC")
    List<Itinerary> findPublicItinerariesByTags(@Param("tags") List<String> tags);

    // Fix the popular itineraries query to use Pageable for the limit
    // Change List<Itinerary> to Page<Itinerary>
    @Query("SELECT i FROM Itinerary i WHERE i.isPublic = true ORDER BY i.likeCount DESC")
    Page<Itinerary> findTopPublicItinerariesByLikes(Pageable pageable);

    @Query("""
                SELECT DISTINCT i FROM Itinerary i
                LEFT JOIN i.tags t
                WHERE i.isPublic = true
                AND (
                    UPPER(i.theme) IN :interestCodes
                    OR UPPER(t) IN :interestCodes
                )
                ORDER BY i.likeCount DESC, i.copyCount DESC
            """)
    List<Itinerary> findRecommended(@Param("interestCodes") List<String> interestCodes, Pageable pageable);


    //#########   For Notification    #########//

    // Find trips starting exactly X days from now (for Reminders)
    List<Itinerary> findAllByStartDate(java.time.LocalDate startDate);

    // Find all itineraries that are currently "in progress" (for Weather Alerts)
    @Query("SELECT i FROM Itinerary i WHERE :today BETWEEN i.startDate AND i.endDate")
    List<Itinerary> findAllActiveItineraries(@Param("today") java.time.LocalDate today);

}