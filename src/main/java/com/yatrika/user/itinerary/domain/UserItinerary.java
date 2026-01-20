package com.yatrika.user.itinerary.domain;

import com.yatrika.destination.domain.Destination;
import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserItinerary extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long userId; // Link to the user who owns this trip

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "userItinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserItineraryItem> items = new ArrayList<>();

    // Helper method
    public void addItem(UserItineraryItem item) {
        item.setUserItinerary(this);
        this.items.add(item);
    }
}
