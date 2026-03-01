package com.yatrika.subscription.repository;

import com.yatrika.subscription.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId); // Adjust ID type to match your BaseEntity
}