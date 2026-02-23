package com.yatrika.itinerary.domain;

import com.yatrika.destination.domain.Destination;
import com.yatrika.shared.domain.BaseEntity;
import com.yatrika.user.domain.User;
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
    @Column(name = "user_id")
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
    @Builder.Default
    private Double averageRating = 0.0;

    // Social Stats
    @Builder.Default
    private Integer copyCount = 0;

    @Builder.Default
    private Integer likeCount = 0;

    @Column(length = 10)
    private String countryCode; // e.g., "NP", "CH", "US"

    @ElementCollection
    @CollectionTable(name = "itinerary_tags", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC, orderInDay ASC")
    @Builder.Default
    private List<ItineraryItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false) // reference to the userId column
    private User user;

    @ManyToMany
    @JoinTable(
            name = "itinerary_destinations",
            joinColumns = @JoinColumn(name = "itinerary_id"),
            inverseJoinColumns = @JoinColumn(name = "destination_id")
    )
    private List<Destination> destinations = new ArrayList<>();

    @OneToMany(mappedBy = "itinerary", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLikedItinerary> likedByUsers;

    @OneToMany(mappedBy = "itinerary", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavedItinerary> savedByUsers;

    @OneToMany(
            mappedBy = "itinerary",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ItineraryImage> images = new ArrayList<>();

    // Helper to add items

    public void addItem(ItineraryItem item) {
        if (item == null) return;
        items.add(item);
        item.setItinerary(this);
    }

    public void removeItem(ItineraryItem item) {
        if (item == null) return;
        items.remove(item);
        item.setItinerary(null);
    }

    public void addImage(ItineraryImage image) {
        if (image == null) return;

        images.add(image);
        image.setItinerary(this);

        if (image.getSortOrder() == null) {
            image.setSortOrder(images.size());
        }
    }

    public void removeImage(ItineraryImage image) {
        if (image == null) return;

        images.remove(image);
        image.setItinerary(null);

        // Re-normalize order
        for (int i = 0; i < images.size(); i++) {
            images.get(i).setSortOrder(i + 1);
        }
    }

    public void setCoverImage(ItineraryImage cover) {
        if (cover == null || !images.contains(cover)) {
            throw new IllegalArgumentException("Image does not belong to this itinerary");
        }

        images.forEach(img -> img.setIsCover(false));
        cover.setIsCover(true);
    }


    // HELPER METHOD to calculate estimated budget from items
    public BigDecimal calculateEstimatedBudget() {
        if (estimatedBudget != null) {
            return estimatedBudget;
        }
        // Calculate from items if not set
        if (items != null && !items.isEmpty()) {
            return items.stream()
                    .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }
}