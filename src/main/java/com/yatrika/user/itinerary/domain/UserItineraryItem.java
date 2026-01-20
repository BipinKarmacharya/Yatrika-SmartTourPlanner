package com.yatrika.user.itinerary.domain;

import com.yatrika.destination.domain.Destination;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@Entity
@Table(name = "user_itinerary_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_itinerary_id")
    private UserItinerary userItinerary;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    private Integer dayNumber;
    private Integer orderInDay;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;

    private String activityType;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
