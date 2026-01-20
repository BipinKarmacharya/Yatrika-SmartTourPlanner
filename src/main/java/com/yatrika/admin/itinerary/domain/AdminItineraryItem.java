package com.yatrika.admin.itinerary.domain;

import com.yatrika.destination.domain.Destination;
import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "admin_itinerary_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"admin_itinerary_id", "dayNumber", "orderInDay"}
                )
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"adminItinerary", "destination"})
public class AdminItineraryItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_itinerary_id")
    private AdminItinerary adminItinerary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private Integer orderInDay;

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column
    private java.time.LocalTime startTime;

    @Column
    private java.time.LocalTime endTime;

    @Column
    private Integer durationMinutes;

    @Column(length = 50)
    private String activityType;

}
