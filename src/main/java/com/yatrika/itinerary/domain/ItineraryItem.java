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

    private Integer dayNumber;
    private Integer orderInDay;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private BigDecimal estimatedCost;
    private LocalTime startTime;
    private LocalTime endTime;
    private String activityType; // VISIT, MEAL, TRANSPORT
    private Boolean isVisited = false;
}