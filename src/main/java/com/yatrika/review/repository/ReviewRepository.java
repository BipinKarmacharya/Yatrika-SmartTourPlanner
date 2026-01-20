package com.yatrika.review.repository;

import com.yatrika.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByDestinationId(Long destinationId, Pageable pageable);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    Optional<Review> findByUserIdAndDestinationId(Long userId, Long destinationId);

    boolean existsByUserIdAndDestinationId(Long userId, Long destinationId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.destination.id = :destinationId")
    Double findAverageRatingByDestinationId(@Param("destinationId") Long destinationId);

    Long countByDestinationId(Long destinationId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT FUNCTION('HOUR', r.createdAt) as hour, COUNT(r) as count FROM Review r " +
            "WHERE r.createdAt >= :startDate GROUP BY FUNCTION('HOUR', r.createdAt) ORDER BY hour")
    List<Object[]> countReviewsByHour(@Param("startDate") LocalDateTime startDate);
}