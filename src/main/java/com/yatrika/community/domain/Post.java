package com.yatrika.community.domain;

import com.yatrika.shared.domain.BaseEntity;
import com.yatrika.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Post extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "trip_duration_days")
    private Integer tripDurationDays;

    @Column(name = "estimated_cost")
    private Double estimatedCost;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "total_views")
    @Builder.Default
    private Integer totalViews = 0;

    @Column(name = "total_likes")
    @Builder.Default
    private Integer totalLikes = 0;

    @Column(name = "destination", length = 100)
    private String destination;

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    // Relationships
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<PostMedia> media = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    @OrderBy("dayNumber ASC")
    private List<PostDay> days = new ArrayList<>();

    // Helper methods
    public void addMedia(PostMedia mediaItem) {
        media.add(mediaItem);
        mediaItem.setPost(this);
    }

    public void addDay(PostDay day) {
        days.add(day);
        day.setPost(this);
    }

    public void incrementViews() {
        this.totalViews++;
    }

    public void incrementLikes() {
        this.totalLikes++;
    }

    public void decrementLikes() {
        if (this.totalLikes > 0) {
            this.totalLikes--;
        }
    }
}