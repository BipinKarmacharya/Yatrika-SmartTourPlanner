package com.yatrika.community.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PostMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType; // image, video

    @Column(length = 255)
    private String caption;

    @Column(name = "day_number")
    private Integer dayNumber; // Which day of the trip this media belongs to

    @Column(name = "display_order")
    private Integer displayOrder;
}