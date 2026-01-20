package com.yatrika.community.repository;

import com.yatrika.community.domain.Post;
import com.yatrika.community.domain.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserId(Long userId, Pageable pageable);

    Page<Post> findByIsPublicTrue(Pageable pageable);

    Page<Post> findByIsPublicTrueAndUserIdNot(Long excludedUserId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isPublic = true AND " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Post> searchPublicPosts(@Param("search") String search, Pageable pageable);

    boolean existsByIdAndUserId(Long postId, Long userId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p WHERE p.isPublic = true ORDER BY p.totalLikes DESC")
    Page<Post> findTrendingPosts(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT FUNCTION('HOUR', p.createdAt) as hour, COUNT(p) as count FROM Post p " +
            "WHERE p.createdAt >= :startDate GROUP BY FUNCTION('HOUR', p.createdAt) ORDER BY hour")
    List<Object[]> countPostsByHour(@Param("startDate") LocalDateTime startDate);
}
