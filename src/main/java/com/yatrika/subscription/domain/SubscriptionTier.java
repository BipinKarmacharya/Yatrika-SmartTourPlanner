package com.yatrika.subscription.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionTier {
    FREE(2, 0L),       // 2 plans, 0 Paisa
    PRO(Integer.MAX_VALUE, 10000L); // Unlimited, 10000 Paisa (Rs. 100)

    private final int maxPlansPerMonth;
    private final long priceInPaisa;
}
