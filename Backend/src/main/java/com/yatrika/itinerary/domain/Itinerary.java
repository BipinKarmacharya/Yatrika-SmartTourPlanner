package com.yatrika.itinerary.domain;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // ❗ Only USERS can have itineraries

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days")
    private Integer totalDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type")
    private TripType tripType;

    @Column(name = "budget_range")
    private String budgetRange;  // LOW, MEDIUM, HIGH, LUXURY

    @Column(name = "estimated_total_cost", precision = 12, scale = 2)
    private BigDecimal estimatedTotalCost;

    @Column(name = "actual_total_cost", precision = 12, scale = 2)
    private BigDecimal actualTotalCost;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ItineraryStatus status = ItineraryStatus.DRAFT;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "total_views")
    @Builder.Default
    private Integer totalViews = 0;

    @Column(name = "total_likes")
    @Builder.Default
    private Integer totalLikes = 0;

    @Column(name = "total_bookmarks")
    @Builder.Default
    private Integer totalBookmarks = 0;

    // Relationships
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("dayNumber ASC, orderInDay ASC")
    private List<ItineraryItem> items = new ArrayList<>();

    // Helper methods
    public void addItem(ItineraryItem item) {
        items.add(item);
        item.setItinerary(this);
    }

    public void removeItem(ItineraryItem item) {
        items.remove(item);
        item.setItinerary(null);
    }

    public boolean isOwner(User user) {
        return this.user != null && this.user.getId().equals(user.getId());
    }

    public boolean canEdit(User user) {
        return isOwner(user) && (status == ItineraryStatus.DRAFT || status == ItineraryStatus.PLANNED);
    }
}
