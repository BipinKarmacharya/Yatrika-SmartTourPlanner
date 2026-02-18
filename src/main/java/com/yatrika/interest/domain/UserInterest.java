package com.yatrika.interest.domain;

import com.yatrika.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_interests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "interest_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id")
    private Interest interest;

    // Convenience constructor
    public UserInterest(User user, Interest interest) {
        this.user = user;
        this.interest = interest;
    }
}
