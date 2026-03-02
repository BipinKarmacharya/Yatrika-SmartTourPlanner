package com.yatrika.interest.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "interests",
        uniqueConstraints = @UniqueConstraint(columnNames = "code")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Interest extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;   // BEACH, ADVENTURE

    @Column(nullable = false, length = 100)
    private String name;   // Beach, Adventure

    @Column(length = 100)
    private String icon;   // optional (beach.svg)

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}

