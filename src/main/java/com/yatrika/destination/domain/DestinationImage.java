package com.yatrika.destination.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "destination_images")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DestinationImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(length = 255)
    private String caption;

    @Column(length = 255)
    private String credits;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "display_order")
    private Integer displayOrder;

    // In a real project, you'd have reference to User who uploaded
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "uploaded_by")
    // private User uploadedBy;
}