package com.yatrika.destination.repository;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.domain.DestinationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    // Basic queries
    List<Destination> findByDistrict(String district);

    List<Destination> findByProvince(String province);

    List<Destination> findByType(DestinationType type);

    Page<Destination> findByType(DestinationType type, Pageable pageable);

    Page<Destination> findByDistrict(String district, Pageable pageable);

    // Search queries
    @Query("SELECT d FROM Destination d WHERE " +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(d.province) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(d.district) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Destination> search(@Param("query") String query, Pageable pageable);

    // Advanced search with multiple filters
    @Query("SELECT d FROM Destination d WHERE " +
            "(:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:district IS NULL OR LOWER(d.district) = LOWER(:district)) AND " +
            "(:province IS NULL OR LOWER(d.province) = LOWER(:province)) AND " +
            "(:type IS NULL OR d.type = :type) AND " +
            "(:category IS NULL OR LOWER(d.category) = LOWER(:category))")
    Page<Destination> advancedSearch(
            @Param("name") String name,
            @Param("district") String district,
            @Param("province") String province,
            @Param("type") DestinationType type,
            @Param("category") String category,
            Pageable pageable
    );

    // Find nearby destinations (simple version - will improve with PostGIS)
    @Query(value = """
        SELECT * FROM destinations d
        WHERE latitude BETWEEN :minLat AND :maxLat
        AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY (POW(69.1 * (latitude - :lat), 2) +
                  POW(69.1 * (:lng - longitude) * COS(latitude / 57.3), 2))
        LIMIT :limit
        """, nativeQuery = true)
    List<Destination> findNearby(
            @Param("lat") BigDecimal latitude,
            @Param("lng") BigDecimal longitude,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("limit") int limit
    );

    // Find popular destinations
    Page<Destination> findByPopularityScoreGreaterThanOrderByPopularityScoreDesc(
            Integer minScore, Pageable pageable);

    // Find top_rated destinations
    Page<Destination> findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(
            BigDecimal minRating, Pageable pageable);

    // Check if destination exists by name in district
    boolean existsByNameAndDistrict(String name, String district);

    // Admin methods
    @Query("SELECT d, COUNT(r) as reviewCount FROM Destination d " +
            "LEFT JOIN Review r ON r.destination.id = d.id " +
            "GROUP BY d.id ORDER BY reviewCount DESC")
    List<Object[]> findPopularDestinationsWithReviewCount();

    @Query("SELECT d, COUNT(ii.itinerary) AS itineraryCount FROM Destination d " +
            // 1. Join Destination (d) with ItineraryItem (ii)
            "LEFT JOIN ItineraryItem ii ON ii.destination = d " +
            // 2. Filter using the Itinerary's creation date (accessed via ii.itinerary)
            "WHERE ii.itinerary.createdAt >= :sinceDate " +
            "GROUP BY d.id ORDER BY itineraryCount DESC")

    List<Object[]> findPopularDestinationsSince(@Param("sinceDate") java.time.LocalDateTime sinceDate);

}
