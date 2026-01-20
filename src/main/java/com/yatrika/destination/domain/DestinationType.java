package com.yatrika.destination.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DestinationType {
    NATURAL,
    CULTURAL,
    ADVENTURE,
    RELIGIOUS,
    HISTORICAL,
    ENTERTAINMENT;

    @JsonCreator
    public static DestinationType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null; // Or return UNKNOWN;
        }
        return DestinationType.valueOf(value.toUpperCase());
    }
}
