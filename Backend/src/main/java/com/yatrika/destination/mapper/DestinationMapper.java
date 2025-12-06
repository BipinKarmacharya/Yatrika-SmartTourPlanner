package com.yatrika.destination.mapper;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.dto.request.DestinationRequest;
import com.yatrika.destination.dto.response.DestinationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DestinationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "operatingHours", ignore = true)

    // Fields that are in Destination but not in DestinationRequest (must be ignored)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "wardNumber", ignore = true)
    @Mapping(target = "fullAddress", ignore = true)
    @Mapping(target = "altitudeMeters", ignore = true)
    @Mapping(target = "bestTimeOfDay", ignore = true)
    @Mapping(target = "googlePlaceId", ignore = true)
    @Mapping(target = "tripadvisorId", ignore = true)
    @Mapping(target = "wikipediaUrl", ignore = true)
    @Mapping(target = "lastVerifiedAt", ignore = true)

    // Fields that are initialized in the Service Layer (must be ignored to prevent builder conflict)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "totalVisits", ignore = true)
    @Mapping(target = "popularityScore", ignore = true)

    Destination toEntity(DestinationRequest request);

    // NOTE: If getLocationString() and isFreeEntry() do not exist on Destination,
    // you must change these to map actual fields or use expressions.
    @Mapping(target = "locationString", expression = "java(destination.getLocationString())")
    @Mapping(target = "freeEntry", expression = "java(destination.isFreeEntry())")
    DestinationResponse toResponse(Destination destination);

    // - Keeps ignore=true for audit and service-managed fields.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "operatingHours", ignore = true)
    @Mapping(target = "lastVerifiedAt", ignore = true)
    // Add ignore for the scores/ratings as they shouldn't be updated via DTO
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "totalVisits", ignore = true)
    @Mapping(target = "popularityScore", ignore = true)

    // Also ignore location/metadata fields if they are not meant to be updated via request
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "wardNumber", ignore = true)
    @Mapping(target = "fullAddress", ignore = true)
    @Mapping(target = "altitudeMeters", ignore = true)
    @Mapping(target = "bestTimeOfDay", ignore = true)
    @Mapping(target = "googlePlaceId", ignore = true)
    @Mapping(target = "tripadvisorId", ignore = true)
    @Mapping(target = "wikipediaUrl", ignore = true)
    void updateEntity(@MappingTarget Destination destination, DestinationRequest request);

    // Add default methods for complex mappings
    default BigDecimal mapBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    default Double mapDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}