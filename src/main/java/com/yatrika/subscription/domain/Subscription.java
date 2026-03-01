package com.yatrika.subscription.domain;

import com.yatrika.shared.domain.BaseEntity;
import com.yatrika.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Subscription extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private SubscriptionTier tier = SubscriptionTier.FREE;// FREE, PRO

    private Integer monthlyPlanCount = 0;

    private LocalDateTime cycleStartDate = LocalDateTime.now();;

    private LocalDateTime lastBillingDate;

    private String lastPidx; // For Khalti verification tracking

    // Helper to check if reset is needed
    public boolean isCycleExpired() {
        return LocalDateTime.now().isAfter(cycleStartDate.plusDays(30));
    }
}
