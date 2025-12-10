package com.yatrika.review.domain;

import com.yatrika.shared.domain.BaseEntity;
import com.yatrika.user.domain.User;
import com.yatrika.destination.domain.Destination;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "destination_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private Integer rating; // 1 to 5

    @Column(length = 2000)
    private String comment;

    @Column(name = "visited_date")
    private LocalDate visitedDate;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    // Helper method
    public boolean isByUser(Long userId) {
        return user != null && user.getId().equals(userId);
    }
}
