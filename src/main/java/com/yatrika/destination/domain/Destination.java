package com.yatrika.destination.domain;

import com.yatrika.shared.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "destinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"images", "operatingHours"})
public class Destination extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String shortDescription;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(length = 100)
    private String municipality;

    private Integer wardNumber;

    private String fullAddress;

    @Min(value = -90, message = "Latitude must be at least -90")
    @Max(value = 90, message = "Latitude must be at most 90")
    @Column(precision = 11, scale = 8)
    private BigDecimal latitude;

    @Min(value = -180, message = "Longitude must be at least -180")
    @Max(value = 180, message = "Longitude must be at most 180")
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Builder.Default
    private Integer altitudeMeters = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DestinationType type;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String subCategory;

    private String bestSeason;

    @Column(name = "best_time_of_day")
    private String bestTimeOfDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level")
    @Builder.Default
    private DifficultyLevel difficultyLevel = DifficultyLevel.MODERATE;

    @Column(name = "average_duration_hours")
    private Integer averageDurationHours;

    @Column(name = "entrance_fee_local")
    @Builder.Default
    private BigDecimal entranceFeeLocal = BigDecimal.ZERO;

    @Column(name = "entrance_fee_foreign")
    @Builder.Default
    private BigDecimal entranceFeeForeign = BigDecimal.ZERO;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "total_visits")
    @Builder.Default
    private Integer totalVisits = 0;

    @Column(name = "popularity_score")
    @Builder.Default
    private Integer popularityScore = 0;

    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "tripadvisor_id")
    private String tripadvisorId;

    @Column(name = "wikipedia_url")
    private String wikipediaUrl;

    @Column(name = "safety_level")
    @Builder.Default
    private Integer safetyLevel = 3;

    @Column(name = "has_parking")
    @Builder.Default
    private Boolean hasParking = false;

    @Column(name = "has_restrooms")
    @Builder.Default
    private Boolean hasRestrooms = false;

    @Column(name = "has_drinking_water")
    @Builder.Default
    private Boolean hasDrinkingWater = false;

    @Column(name = "has_wifi")
    @Builder.Default
    private Boolean hasWifi = false;

    @Column(name = "has_guide_services")
    @Builder.Default
    private Boolean hasGuideServices = false;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    // Relationships
    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DestinationImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OperatingHour> operatingHours = new ArrayList<>();

    // Helper methods
    public void addImage(DestinationImage image) {
        images.add(image);
        image.setDestination(this);
    }
    public void removeImage(DestinationImage image) {
        images.remove(image);
        image.setDestination(null);
    }

    public void addOperatingHour(OperatingHour hour) {
        operatingHours.add(hour);
        hour.setDestination(this);
    }

    public String getLocationString() {
        return String.format("%s, %s Province", district, province);
    }

    public boolean isFreeEntry() {
        return (entranceFeeLocal == null || entranceFeeLocal.compareTo(BigDecimal.ZERO) == 0) &&
                (entranceFeeForeign == null || entranceFeeForeign.compareTo(BigDecimal.ZERO) == 0);
    }
}
