package com.yatrika.itinerary.domain;

import com.yatrika.destination.domain.Destination;
import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "itinerary_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ItineraryItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;  // Day 1, Day 2, etc.

    @Column(name = "order_in_day")
    private Integer orderInDay;  // Sequence for the day

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "activity_type", length = 50)
    private String activityType;  // VISIT, MEAL, TRANSPORT, ACCOMMODATION, ACTIVITY

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    private String notes;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "location_address")
    private String locationAddress;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "is_cancelled")
    @Builder.Default
    private Boolean isCancelled = false;
}
