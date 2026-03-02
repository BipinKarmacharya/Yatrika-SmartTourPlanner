package com.yatrika.notification.domain;

import com.yatrika.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_notification_preferences")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class NotificationPreference {
    @Id
    private Long userId; // One-to-one with User ID

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean tripReminders = true;
    private boolean weatherAlerts = true;
    private boolean communityUpdates = true;
}