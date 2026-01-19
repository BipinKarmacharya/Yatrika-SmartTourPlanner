package com.yatrika.admin.itinerary.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin_itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = "items")
public class AdminItinerary extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer totalDays;

    @Column(length = 50)
    private String theme; // Adventure, Cultural, Religious

    private BigDecimal estimatedBudget;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Ordered itinerary items (day-wise destinations)
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "adminItinerary",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("dayNumber ASC, orderInDay ASC")
    private List<AdminItineraryItem> items = new ArrayList<>();

    /* Convenience methods */

    public void addItem(AdminItineraryItem item) {
        items.add(item);
        item.setAdminItinerary(this);
    }

    public void removeItem(AdminItineraryItem item) {
        items.remove(item);
        item.setAdminItinerary(null);
    }
}
