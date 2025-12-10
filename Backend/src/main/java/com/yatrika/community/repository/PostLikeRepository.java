package com.yatrika.community.repository;

import com.yatrika.community.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
}