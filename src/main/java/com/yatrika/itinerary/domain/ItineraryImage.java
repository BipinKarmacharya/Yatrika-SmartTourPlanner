package com.yatrika.itinerary.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "itinerary_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@AttributeOverride(name = "createdAt", column = @Column(name = "created_at", insertable = false, updatable = false))
@AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
public class ItineraryImage extends BaseEntity {

    @Column(nullable = false)
    private String url;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    private Boolean isCover = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    @JsonIgnore
    private Itinerary itinerary;
}

