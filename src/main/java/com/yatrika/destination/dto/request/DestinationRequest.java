package com.yatrika.destination.dto.request;

import com.yatrika.destination.domain.DestinationType;
import com.yatrika.destination.domain.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DestinationRequest {

    @NotBlank(message = "Destination name is required")
    @Length(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @Length(max = 500, message = "Short description must be less than 500 characters")
    private String shortDescription;

    private String description;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    private String municipality;

    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    @NotNull(message = "Destination type is required")
    private DestinationType type;

    private String category;

    private String subCategory;

    private String bestSeason;

    private DifficultyLevel difficultyLevel;

    private Integer averageDurationHours;

    private BigDecimal entranceFeeLocal;

    private BigDecimal entranceFeeForeign;

    private String[] tags;

    private Integer safetyLevel;

    private Boolean hasParking;

    private Boolean hasRestrooms;

    private Boolean hasDrinkingWater;

    private Boolean hasWifi;

    private Boolean hasGuideServices;

    private List<ImageRequest> images;

    @Data
    public static class ImageRequest {
        private String imageUrl;
        private String caption;
        private Boolean isPrimary;
    }
}
