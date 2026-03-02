package com.yatrika.shared.exception;

public class SubscriptionLimitExceededException extends RuntimeException {
    public SubscriptionLimitExceededException() {
        super("FREE_LIMIT_REACHED");
    }
}
