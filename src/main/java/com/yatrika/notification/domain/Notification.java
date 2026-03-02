package com.yatrika.notification.domain;

import com.yatrika.shared.domain.BaseEntity;
import com.yatrika.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The recipient

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type; // TRIP_REMINDER, WEATHER_ALERT, SYSTEM_UPDATE

    private boolean isRead = false;

    // Optional: Link to a specific trip so clicking the notification opens that trip
    private Long referenceId;


    public enum NotificationType {
        TRIP_REMINDER,
        WEATHER_ALERT,
        FLIGHT_UPDATE,
        MARKETING
    }
}
