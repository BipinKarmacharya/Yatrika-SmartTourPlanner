package com.yatrika.community.repository;

import com.yatrika.community.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Efficiently fetch user details with the post to avoid N+1 issues in lists
    @EntityGraph(attributePaths = {"user"})
    Page<Post> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Post> findByIsPublicTrue(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isPublic = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.destination) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Post> searchByKeyword(@Param("query") String query, Pageable pageable);

    // Optimized trending: Likes + Views weight
    @Query("SELECT p FROM Post p WHERE p.isPublic = true ORDER BY (p.totalLikes * 2 + p.totalViews) DESC")
    Page<Post> findTrendingPosts(Pageable pageable);

    // Critical for Single Post View: Fetch everything in one go
    @EntityGraph(attributePaths = {"user", "media", "tags", "days"})
    Optional<Post> findById(Long id);

    // Simplified standard derived query
    long countByUserId(Long userId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT FUNCTION('HOUR', p.createdAt) as hour, COUNT(p) as count FROM Post p " +
            "WHERE p.createdAt >= :startDate GROUP BY FUNCTION('HOUR', p.createdAt) ORDER BY hour")
    List<Object[]> countPostsByHour(@Param("startDate") LocalDateTime startDate);
}