package com.yatrika.itinerary.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Itinerary extends BaseEntity {

    // Identity & Ownership
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // null if it's an Admin Template
    private Long userId;

    // Metadata
    @Enumerated(EnumType.STRING)
    private ItineraryStatus status; // TEMPLATE, DRAFT, etc.

    @Builder.Default
    private Boolean isPublic = false;

    @Builder.Default
    private Boolean isAdminCreated = false;

    // --- COPY TRACKING LOGIC ---
    // Tracks the ID of the original Admin or Public trip this was copied from
    private Long sourceId;

    // Dates & Costs
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private String theme;
    private BigDecimal estimatedBudget;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    // Social Stats
    @Builder.Default
    private Integer copyCount = 0;
    @Builder.Default
    private Integer likeCount = 0;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC, orderInDay ASC")
    @Builder.Default
    private List<ItineraryItem> items = new ArrayList<>();

    // Helper to add items
    public void addItem(ItineraryItem item) {
        items.add(item);
        item.setItinerary(this);
    }
}