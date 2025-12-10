package com.yatrika.community.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "post_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PostDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "activities")
    private String activities; // Comma-separated activities

    @Column(name = "accommodation")
    private String accommodation;

    @Column(name = "food")
    private String food;

    @Column(name = "transportation")
    private String transportation;
}