package com.yatrika.destination.service;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.domain.DestinationImage;
import com.yatrika.destination.dto.request.DestinationRequest;
import com.yatrika.destination.dto.request.DestinationSearchRequest;
import com.yatrika.destination.dto.request.NearbySearchRequest;
import com.yatrika.destination.dto.response.BulkDestinationResult;
import com.yatrika.destination.dto.response.DestinationResponse;
import com.yatrika.destination.mapper.DestinationMapper;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.shared.service.FileStorageService;
import com.yatrika.user.domain.User;
import com.yatrika.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationMapper destinationMapper;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * Create a new destination using URLs provided in the request.
     * The files should have been uploaded previously via FileUploadController.
     */
    @Transactional
    public DestinationResponse createDestination(DestinationRequest request) {
        log.info("Creating new destination: {}", request.getName());

        if (destinationRepository.existsByNameAndDistrict(request.getName(), request.getDistrict())) {
            throw new AppException("Destination with this name already exists in " + request.getDistrict());
        }

        Destination destination = destinationMapper.toEntity(request);

        // Initialize admin-managed stats
        destination.setPopularityScore(0);
        destination.setTotalReviews(0);
        destination.setTotalVisits(0);
        destination.setAverageRating(BigDecimal.ZERO);

        // Map Image URLs to Entities
        if (request.getImages() != null) {
            request.getImages().forEach(imgDto -> {
                DestinationImage img = DestinationImage.builder()
                        .imageUrl(imgDto.getImageUrl())
                        .caption(imgDto.getCaption())
                        .isPrimary(imgDto.getIsPrimary())
                        .destination(destination)
                        .build();
                destination.addImage(img);
            });
        }

        Destination savedDestination = destinationRepository.save(destination);
        return destinationMapper.toResponse(savedDestination);
    }

    /**
     * Bulk creation now simply calls the unified createDestination method.
     */
    @Transactional
    public BulkDestinationResult bulkCreate(List<DestinationRequest> requests) {
        var successes = new java.util.ArrayList<BulkDestinationResult.SuccessItem>();
        var failures = new java.util.ArrayList<BulkDestinationResult.FailureItem>();

        for (int i = 0; i < requests.size(); i++) {
            DestinationRequest request = requests.get(i);
            try {
                DestinationResponse response = createDestination(request);
                successes.add(BulkDestinationResult.SuccessItem.builder()
                        .index(i).destinationId(response.getId()).name(response.getName()).build());
            } catch (Exception ex) {
                failures.add(BulkDestinationResult.FailureItem.builder()
                        .index(i).name(request.getName()).error(ex.getMessage()).build());
            }
        }

        return BulkDestinationResult.builder()
                .total(requests.size()).success(successes.size()).failed(failures.size())
                .successes(successes).failures(failures).build();
    }

    @Transactional
    @CacheEvict(value = "destinations", key = "#id")
    public DestinationResponse updateDestination(Long id, DestinationRequest request) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));

        // Uniqueness validation
        if (!destination.getName().equals(request.getName()) &&
                destinationRepository.existsByNameAndDistrict(request.getName(), request.getDistrict())) {
            throw new AppException("Another destination with this name already exists in " + request.getDistrict());
        }

        destinationMapper.updateEntity(destination, request);

        if (request.getImages() != null) {
            updateDestinationImages(destination, request.getImages());
        }

        return destinationMapper.toResponse(destinationRepository.save(destination));
    }

    private void updateDestinationImages(Destination destination, List<DestinationRequest.ImageRequest> newImages) {
        List<String> incomingUrls = newImages.stream()
                .map(DestinationRequest.ImageRequest::getImageUrl).toList();

        // Identify images to remove
        List<DestinationImage> toRemove = destination.getImages().stream()
                .filter(existingImg -> !incomingUrls.contains(existingImg.getImageUrl()))
                .toList();

        // Cleanup physical files and DB relations
        for (DestinationImage img : toRemove) {
            fileStorageService.deleteFile(img.getImageUrl());
            destination.removeImage(img);
        }

        // Add or Update images
        for (DestinationRequest.ImageRequest imgDto : newImages) {
            destination.getImages().stream()
                    .filter(e -> e.getImageUrl().equals(imgDto.getImageUrl()))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> {
                                existing.setCaption(imgDto.getCaption());
                                existing.setIsPrimary(imgDto.getIsPrimary());
                            },
                            () -> {
                                DestinationImage newImg = DestinationImage.builder()
                                        .imageUrl(imgDto.getImageUrl())
                                        .caption(imgDto.getCaption())
                                        .isPrimary(imgDto.getIsPrimary())
                                        .destination(destination)
                                        .build();
                                destination.addImage(newImg);
                            }
                    );
        }
    }

    @Transactional
    @CacheEvict(value = "destinations", key = "#id")
    public void deleteDestination(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));

        // Collect URLs before deletion
        List<String> urlsToDelete = destination.getImages().stream()
                .map(DestinationImage::getImageUrl).toList();

        destinationRepository.delete(destination);

        // Safe physical deletion
        urlsToDelete.forEach(fileStorageService::deleteFile);
        log.info("Destination and images deleted: {}", id);
    }

    // --- Search & Fetch Methods (Unchanged, but remain robust) ---

    @Cacheable(value = "destinations", key = "#id")
    public DestinationResponse getDestinationById(Long id) {
        return destinationRepository.findById(id)
                .map(destinationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));
    }

    public Page<DestinationResponse> getAllDestinations(Pageable pageable) {
        return destinationRepository.findAll(pageable).map(destinationMapper::toResponse);
    }


    public Page<DestinationResponse> getRecommendationsForUser(Long userId, Pageable pageable) {
        log.info("Fetching recommendations for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> interests = user.getInterests();

        Page<Destination> destinations;

        // 1. Logic: If user has no interests, show them popular destinations instead
        if (interests == null || interests.isEmpty()) {
            log.debug("User has no explicit interests, falling back to popular destinations.");
            return getPopularDestinations(pageable);
        } else {
            // 2. Query: Find destinations where tags overlap with user interests
            destinations = destinationRepository.findRecommendedDestinations(
                    interests.toArray(new String[0]),
                    10, // Limit to top 10 matches
                    pageable
            );
        }

        // 3. Mapping: Convert the Page of Database Entities to a Page of Response DTOs
        // We use destinationMapper::toResponse which is already injected in your class
        return destinations.map(destinationMapper::toResponse);
    }

    // ... searchDestinations, findNearbyDestinations, etc. follow the same pattern ...
    // --- Advanced Search and Filtering ---

    @Cacheable(value = "destinationSearch", key = "#request.hashCode()")
    public Page<DestinationResponse> searchDestinations(DestinationSearchRequest request) {
        log.debug("Searching destinations with criteria: {}", request);

        Pageable pageable = request.toPageable();

        // Convert List<String> to String[] for the native PostgreSQL query
        String[] tagsArray = (request.getTags() != null && !request.getTags().isEmpty())
                ? request.getTags().toArray(new String[0])
                : null;

        // Ensure the arguments match the Repository definition exactly:
        // String, String, BigDecimal, BigDecimal, String[], Pageable
        Page<Destination> destinations = destinationRepository.advancedSearch(
                request.getName(),
                request.getDistrict(),
                request.getMinPrice(),
                request.getMaxPrice(),
                tagsArray,
                pageable
        );

        return destinations.map(destinationMapper::toResponse);
    }

    /**
     * Finds destinations within a specific radius using bounding box logic.
     */
    public List<DestinationResponse> findNearbyDestinations(NearbySearchRequest request) {
        log.debug("Finding destinations near ({}, {}) within {}km",
                request.getLatitude(), request.getLongitude(), request.getRadiusKm());

        // Calculate bounding box for performance optimization in SQL
        BigDecimal lat = request.getLatitude();
        BigDecimal lng = request.getLongitude();
        double radiusDegrees = request.getRadiusKm() / 111.0; // Approximation: 1 degree ≈ 111km

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

    // --- Specialized Collections ---

    @Cacheable(value = "popularDestinations", key = "#pageable.pageNumber")
    public Page<DestinationResponse> getPopularDestinations(Pageable pageable) {
        log.debug("Fetching popular destinations (Score > 50)");

        // Assuming 50 is your threshold for "Popular"
        Page<Destination> destinations = destinationRepository
                .findByPopularityScoreGreaterThanOrderByPopularityScoreDesc(50, pageable);

        return destinations.map(destinationMapper::toResponse);
    }

    @Cacheable(value = "topRatedDestinations", key = "#pageable.pageNumber")
    public Page<DestinationResponse> getTopRatedDestinations(Pageable pageable) {
        log.debug("Fetching top rated destinations (Rating >= 4.0)");

        Page<Destination> destinations = destinationRepository
                .findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(
                        new BigDecimal("4.0"), pageable);

        return destinations.map(destinationMapper::toResponse);
    }

    @Cacheable(value = "destinationsByDistrict", key = "#district + '-' + #pageable.pageNumber")
    public Page<DestinationResponse> getDestinationsByDistrict(String district, Pageable pageable) {
        log.debug("Fetching destinations in district: {}", district);

        Page<Destination> destinations = destinationRepository.findByDistrict(district, pageable);
        return destinations.map(destinationMapper::toResponse);
    }

    /**
     * Internal helper to fetch the raw entity when other services
     * (like ReviewService) need to link to a Destination.
     */
    public Destination getDestinationEntity(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));
    }
}