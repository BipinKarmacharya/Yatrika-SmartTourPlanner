package com.yatrika.community.domain;

import com.yatrika.shared.domain.BaseEntity;
import com.yatrika.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"post_id", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PostLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}