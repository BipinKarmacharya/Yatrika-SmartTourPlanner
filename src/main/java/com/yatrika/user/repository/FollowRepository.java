package com.yatrika.user.repository;

import com.yatrika.user.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Long countByFollowingId(Long userId); // Followers count
    Long countByFollowerId(Long userId);  // Following count
}