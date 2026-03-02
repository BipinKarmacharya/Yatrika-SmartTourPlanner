package com.yatrika.subscription.service;

import com.yatrika.shared.exception.SubscriptionLimitExceededException;
import com.yatrika.subscription.domain.Subscription;
import com.yatrika.subscription.domain.SubscriptionTier;
import com.yatrika.subscription.repository.SubscriptionRepository;
import com.yatrika.user.domain.User;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void validateAndTrackUsage(Long userId) {

        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSubscription(userId));

        // Reset cycle if 30 days passed
        if (sub.getCycleStartDate().plusDays(30).isBefore(LocalDateTime.now())) {
            sub.setMonthlyPlanCount(0);
            sub.setCycleStartDate(LocalDateTime.now());
        }

        if (sub.getTier() == SubscriptionTier.FREE && sub.getMonthlyPlanCount() >= 2) {
            throw new SubscriptionLimitExceededException();
        }

        sub.setMonthlyPlanCount(sub.getMonthlyPlanCount() + 1);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void upgradeToPro(Long userId) {

        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSubscription(userId));

        sub.setTier(SubscriptionTier.PRO);
        sub.setMonthlyPlanCount(0);
        subscriptionRepository.save(sub);
    }

    private Subscription createDefaultSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return subscriptionRepository.save(
                Subscription.builder()
                        .user(user)
                        .tier(SubscriptionTier.FREE)
                        .monthlyPlanCount(0)
                        .cycleStartDate(LocalDateTime.now())
                        .build()
        );
    }
}