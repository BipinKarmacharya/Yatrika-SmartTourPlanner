package com.yatrika.destination.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@Entity
@Table(name = "operating_hours")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHour extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 0=Sunday, 6=Saturday

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "is_closed")
    private Boolean isClosed;

    @Column(length = 255)
    private String notes;

    // Helper methods
    public String getDayName() {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek];
    }

    public boolean isOpenNow() {
        if (isClosed != null && isClosed) return false;
        if (opensAt == null || closesAt == null) return true; // Assume always open

        LocalTime now = LocalTime.now();
        return !now.isBefore(opensAt) && !now.isAfter(closesAt);
    }
}