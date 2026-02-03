package com.yatrika.itinerary.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_liked_itineraries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"itinerary_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLikedItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "liked_at", nullable = false)
    private LocalDateTime likedAt;
}

