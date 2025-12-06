package com.yatrika.destination.service;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.dto.request.DestinationRequest;
import com.yatrika.destination.dto.request.DestinationSearchRequest;
import com.yatrika.destination.dto.request.NearbySearchRequest;
import com.yatrika.destination.dto.response.DestinationResponse;
import com.yatrika.destination.mapper.DestinationMapper;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationMapper destinationMapper;

    // Create a new destination
    @Transactional
    public DestinationResponse createDestination(DestinationRequest request) {
        log.info("Creating new destination: {}", request.getName());

        // Check if destination already exists in the same district
        if (destinationRepository.existsByNameAndDistrict(request.getName(), request.getDistrict())) {
            throw new AppException("Destination with this name already exists in " + request.getDistrict());
        }

        Destination destination = destinationMapper.toEntity(request);
        destination.setPopularityScore(0);
        destination.setTotalReviews(0);
        destination.setTotalVisits(0);
        destination.setAverageRating(BigDecimal.ZERO);

        Destination saveDestination = destinationRepository.save(destination);
        log.info("Destination created successfully: {} (ID: {})",
                saveDestination.getName(), saveDestination.getId());

        return destinationMapper.toResponse(saveDestination);
    }

    public Page<DestinationResponse> getAllDestinations(Pageable pageable) {
        log.debug("Getting all destinations");
        Page<Destination> destinations = destinationRepository.findAll(pageable);
        return destinations.map(destinationMapper::toResponse);
    }

    public Destination getDestinationEntity(Long id) {
        log.debug("Fetching Destination entity with ID: {}", id);
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));
    }

    // Get destination by ID
    @Cacheable(value = "destinations", key = "#id")
    public DestinationResponse getDestinationById(Long id) {
        log.debug("Fetching destination with ID: {}", id);

        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));

        return destinationMapper.toResponse(destination);
    }

    // Update destination
    @Transactional
    @CacheEvict(value = "destinations", key = "#id")
    public DestinationResponse updateDestination(Long id, DestinationRequest request) {
        log.info("Updating destination ID: {}", id);

        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));

        // Check if name already exists (excluding current destination)
        if (!destination.getName().equals(request.getName()) &&
                destinationRepository.existsByNameAndDistrict(request.getName(), request.getDistrict())) {
            throw new AppException("Another destination with this name already exists in " + request.getDistrict());
        }

        destinationMapper.updateEntity(destination, request);
        Destination updatedDestination = destinationRepository.save(destination);

        log.info("Destination updated successfully: {}", updatedDestination.getName());
        return destinationMapper.toResponse(updatedDestination);
    }

    // Delete destination
    @Transactional
    @CacheEvict(value = "destinations", key = "#id")
    public void deleteDestination(Long id) {
        log.info("Deleting destination ID: {}", id);

        if (!destinationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Destination", "id", id);
        }

        destinationRepository.deleteById(id);
        log.info("Destination deleted successfully: {}", id);
    }

    // Search destinations
    @Cacheable(value = "destinationSearch", key = "#request.hashCode()")
    public Page<DestinationResponse> searchDestinations(DestinationSearchRequest request) {
        log.debug("Searching destinations with criteria: {}", request);

        Pageable pageable = request.toPageable();
        Page<Destination> destinations = destinationRepository.advancedSearch(
                request.getName(),
                request.getDistrict(),
                request.getProvince(),
                request.getType(),
                request.getCategory(),
                pageable
        );

        return destinations.map(destinationMapper::toResponse);
    }

    // Find nearby destinations
    public List<DestinationResponse> findNearbyDestinations(NearbySearchRequest request) {
        log.debug("Finding destinations near ({}, {}) within {}km",
                request.getLatitude(), request.getLongitude(), request.getRadiusKm());

        // Calculate bounding box (simple approximation)
        BigDecimal lat = request.getLatitude();
        BigDecimal lng = request.getLongitude();
        double radiusDegrees = request.getRadiusKm() / 111.0; // Rough conversion

        BigDecimal minLat = lat.subtract(BigDecimal.valueOf(radiusDegrees));
        BigDecimal maxLat = lat.add(BigDecimal.valueOf(radiusDegrees));
        BigDecimal minLng = lng.subtract(BigDecimal.valueOf(radiusDegrees));
        BigDecimal maxLng = lng.add(BigDecimal.valueOf(radiusDegrees));

        List<Destination> destinations = destinationRepository.findNearby(
                lat, lng, minLat, maxLat, minLng, maxLng, request.getLimit()
        );

        return destinations.stream()
                .map(destinationMapper::toResponse)
                .toList();
    }

    // Get popular destinations
    @Cacheable(value = "popularDestinations", key = "#pageable.pageNumber")
    public Page<DestinationResponse> getPopularDestinations(Pageable pageable) {
        log.debug("Fetching popular destinations");

        Page<Destination> destinations = destinationRepository
                .findByPopularityScoreGreaterThanOrderByPopularityScoreDesc(50, pageable);

        return destinations.map(destinationMapper::toResponse);
    }

    // Get top_rated destinations
    @Cacheable(value = "topRatedDestinations", key = "#pageable.pageNumber")
    public Page<DestinationResponse> getTopRatedDestinations(Pageable pageable) {
        log.debug("Fetching top rated destinations");

        Page<Destination> destinations = destinationRepository
                .findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(
                        new BigDecimal("4.0"), pageable);

        return destinations.map(destinationMapper::toResponse);
    }

    // Get destinations by district
    @Cacheable(value = "destinationsByDistrict", key = "#district + '-' + #pageable.pageNumber")
    public Page<DestinationResponse> getDestinationsByDistrict(String district, Pageable pageable) {
        log.debug("Fetching destinations in district: {}", district);

        Page<Destination> destinations = destinationRepository.findByDistrict(district, pageable);
        return destinations.map(destinationMapper::toResponse);
    }
}
