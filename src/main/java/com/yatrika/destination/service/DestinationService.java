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
    private final FileStorageService fileStorageService;

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

    // ... searchDestinations, findNearbyDestinations, etc. follow the same pattern ...
    // --- Advanced Search and Filtering ---

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

//package com.yatrika.destination.service;
//
//import com.yatrika.destination.domain.Destination;
//import com.yatrika.destination.domain.DestinationImage;
//import com.yatrika.destination.dto.request.DestinationRequest;
//import com.yatrika.destination.dto.request.DestinationSearchRequest;
//import com.yatrika.destination.dto.request.NearbySearchRequest;
//import com.yatrika.destination.dto.response.BulkDestinationResult;
//import com.yatrika.destination.dto.response.DestinationResponse;
//import com.yatrika.destination.mapper.DestinationMapper;
//import com.yatrika.destination.repository.DestinationRepository;
//import com.yatrika.shared.exception.AppException;
//import com.yatrika.shared.exception.ResourceNotFoundException;
//import com.yatrika.shared.service.FileStorageService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class DestinationService {
//
//    private final DestinationRepository destinationRepository;
//    private final DestinationMapper destinationMapper;
//    private final FileStorageService fileStorageService;
//
//    // Create a new destination
//    @Transactional
//    public DestinationResponse createDestination(DestinationRequest request) {
//        log.info("Creating new destination: {}", request.getName());
//
//        // Check if destination already exists in the same district
//        if (destinationRepository.existsByNameAndDistrict(request.getName(), request.getDistrict())) {
//            throw new AppException("Destination with this name already exists in " + request.getDistrict());
//        }
//
//        Destination destination = destinationMapper.toEntity(request);
//        destination.setPopularityScore(0);
//        destination.setTotalReviews(0);
//        destination.setTotalVisits(0);
//        destination.setAverageRating(BigDecimal.ZERO);
//
//        if (request.getImages() != null) {
//            request.getImages().forEach(imgDto -> {
//                DestinationImage img = DestinationImage.builder()
//                        .imageUrl(imgDto.getImageUrl())
//                        .caption(imgDto.getCaption())
//                        .isPrimary(imgDto.getIsPrimary())
//                        .destination(destination)
//                        .build();
//                destination.addImage(img);
//            });
//        }
//
//        Destination saveDestination = destinationRepository.save(destination);
//        log.info("Destination created successfully: {} (ID: {})",
//                saveDestination.getName(), saveDestination.getId());
//
//        return destinationMapper.toResponse(saveDestination);
//    }
//
//    @Transactional
//    public DestinationResponse createWithImages(DestinationRequest request, MultipartFile[] files) {
//        List<DestinationRequest.ImageRequest> imageRequests = new ArrayList<>();
//        List<String> uploadedUrls = new ArrayList<>(); // Track for cleanup
//
//        try {
//            if (files != null && files.length > 0) {
//                for (int i = 0; i < files.length; i++) {
//                    // 1. Store the file
//                    String fileUrl = fileStorageService.storeFile(files[i], "destinations");
//                    uploadedUrls.add(fileUrl);
//
//                    // 2. Build the ImageRequest
//                    DestinationRequest.ImageRequest imgReq = new DestinationRequest.ImageRequest();
//                    imgReq.setImageUrl(fileUrl);
//                    imgReq.setCaption(request.getName() + " - Image " + (i + 1));
//                    imgReq.setIsPrimary(i == 0);
//
//                    imageRequests.add(imgReq);
//                }
//            }
//
//            // 3. Attach to DTO and Save
//            request.setImages(imageRequests);
//            return createDestination(request);
//
//        } catch (Exception e) {
//            // 4. CLEANUP: If anything fails (DB error, validation, etc.), delete the files we just saved
//            log.error("Failed to create destination. Cleaning up {} uploaded files.", uploadedUrls.size());
//            for (String url : uploadedUrls) {
//                fileStorageService.deleteFile(url);
//            }
//            // Re-throw the exception so the user gets the error message and @Transactional rolls back
//            throw e;
//        }
//    }
//
//    @Transactional
//    public BulkDestinationResult bulkCreate(List<DestinationRequest> requests) {
//
//        var successes = new java.util.ArrayList<BulkDestinationResult.SuccessItem>();
//        var failures = new java.util.ArrayList<BulkDestinationResult.FailureItem>();
//
//        for (int i = 0; i < requests.size(); i++) {
//            DestinationRequest request = requests.get(i);
//
//            try {
//                DestinationResponse response = createDestination(request);
//
//                successes.add(BulkDestinationResult.SuccessItem.builder()
//                        .index(i)
//                        .destinationId(response.getId())
//                        .name(response.getName())
//                        .build());
//
//            } catch (Exception ex) {
//                failures.add(BulkDestinationResult.FailureItem.builder()
//                        .index(i)
//                        .name(request.getName())
//                        .error(ex.getMessage())
//                        .build());
//
//                log.warn("Bulk create failed at index {} for destination {}: {}",
//                        i, request.getName(), ex.getMessage());
//            }
//        }
//
//        return BulkDestinationResult.builder()
//                .total(requests.size())
//                .success(successes.size())
//                .failed(failures.size())
//                .successes(successes)
//                .failures(failures)
//                .build();
//    }
//
//
//    public Page<DestinationResponse> getAllDestinations(Pageable pageable) {
//        log.debug("Getting all destinations");
//        Page<Destination> destinations = destinationRepository.findAll(pageable);
//        return destinations.map(destinationMapper::toResponse);
//    }
//
//    public Destination getDestinationEntity(Long id) {
//        log.debug("Fetching Destination entity with ID: {}", id);
//        return destinationRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));
//    }
//
//    // Get destination by ID
//    @Cacheable(value = "destinations", key = "#id")
//    public DestinationResponse getDestinationById(Long id) {
//        log.debug("Fetching destination with ID: {}", id);
//
//        Destination destination = destinationRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));
//
//        return destinationMapper.toResponse(destination);
//    }
//
//
//    @Transactional
//    @CacheEvict(value = "destinations", key = "#id")
//    public DestinationResponse updateDestination(Long id, DestinationRequest request) {
//        Destination destination = destinationRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));
//
//        // 1. Validation for unique name/district
//        if (!destination.getName().equals(request.getName()) &&
//                destinationRepository.existsByNameAndDistrict(request.getName(), request.getDistrict())) {
//            throw new AppException("Another destination with this name already exists in " + request.getDistrict());
//        }
//
//        // 2. Map basic fields
//        destinationMapper.updateEntity(destination, request);
//
//        // 3. Sync Images (Only if images list is provided in request)
//        if (request.getImages() != null) {
//            updateDestinationImages(destination, request.getImages());
//        }
//
//        Destination updatedDestination = destinationRepository.save(destination);
//        return destinationMapper.toResponse(updatedDestination);
//    }
//
//    private void updateDestinationImages(Destination destination, List<DestinationRequest.ImageRequest> newImages) {
//        // Identify URLs that are being removed
//        List<String> incomingUrls = newImages.stream()
//                .map(DestinationRequest.ImageRequest::getImageUrl)
//                .toList();
//
//        List<DestinationImage> toRemove = destination.getImages().stream()
//                .filter(existingImg -> !incomingUrls.contains(existingImg.getImageUrl()))
//                .toList();
//
//        // Remove from DB and physical storage
//        for (DestinationImage img : toRemove) {
//            fileStorageService.deleteFile(img.getImageUrl()); // Delete physical file
//            destination.getImages().remove(img);             // Remove from relationship
//            img.setDestination(null);
//        }
//
//        // Add new ones
//        for (DestinationRequest.ImageRequest imgDto : newImages) {
//            boolean exists = destination.getImages().stream()
//                    .anyMatch(existing -> existing.getImageUrl().equals(imgDto.getImageUrl()));
//
//            if (!exists) {
//                DestinationImage newImg = DestinationImage.builder()
//                        .imageUrl(imgDto.getImageUrl())
//                        .caption(imgDto.getCaption())
//                        .isPrimary(imgDto.getIsPrimary())
//                        .destination(destination)
//                        .build();
//                destination.addImage(newImg);
//            } else {
//                // Optional: Update caption/isPrimary for existing images
//                destination.getImages().stream()
//                        .filter(e -> e.getImageUrl().equals(imgDto.getImageUrl()))
//                        .findFirst()
//                        .ifPresent(e -> {
//                            e.setCaption(imgDto.getCaption());
//                            e.setIsPrimary(imgDto.getIsPrimary());
//                        });
//            }
//        }
//    }
//
//    // Delete destination
//    @Transactional
//    @CacheEvict(value = "destinations", key = "#id")
//    public void deleteDestination(Long id) {
//        log.info("Deleting destination ID: {}", id);
//
//        Destination destination = destinationRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", id));
//
//        // Delete physical files
//        if (destination.getImages() != null) {
//            destination.getImages().forEach(img -> {
//                try {
//                    fileStorageService.deleteFile(img.getImageUrl());
//                } catch (Exception e) {
//                    // Log and continue so the DB record still gets deleted
//                    log.warn("Could not delete physical file: {}. It may have been already deleted.",
//                            img.getImageUrl());
//                }
//            });
//        }
//
//        destinationRepository.delete(destination);
//        log.info("Destination and associated images deleted successfully for ID: {}", id);
//    }
//
//    // Search destinations
//    @Cacheable(value = "destinationSearch", key = "#request.hashCode()")
//    public Page<DestinationResponse> searchDestinations(DestinationSearchRequest request) {
//        log.debug("Searching destinations with criteria: {}", request);
//
//        Pageable pageable = request.toPageable();
//        Page<Destination> destinations = destinationRepository.advancedSearch(
//                request.getName(),
//                request.getDistrict(),
//                request.getProvince(),
//                request.getType(),
//                request.getCategory(),
//                pageable
//        );
//
//        return destinations.map(destinationMapper::toResponse);
//    }
//
//    // Find nearby destinations
//    public List<DestinationResponse> findNearbyDestinations(NearbySearchRequest request) {
//        log.debug("Finding destinations near ({}, {}) within {}km",
//                request.getLatitude(), request.getLongitude(), request.getRadiusKm());
//
//        // Calculate bounding box (simple approximation)
//        BigDecimal lat = request.getLatitude();
//        BigDecimal lng = request.getLongitude();
//        double radiusDegrees = request.getRadiusKm() / 111.0; // Rough conversion
//
//        BigDecimal minLat = lat.subtract(BigDecimal.valueOf(radiusDegrees));
//        BigDecimal maxLat = lat.add(BigDecimal.valueOf(radiusDegrees));
//        BigDecimal minLng = lng.subtract(BigDecimal.valueOf(radiusDegrees));
//        BigDecimal maxLng = lng.add(BigDecimal.valueOf(radiusDegrees));
//
//        List<Destination> destinations = destinationRepository.findNearby(
//                lat, lng, minLat, maxLat, minLng, maxLng, request.getLimit()
//        );
//
//        return destinations.stream()
//                .map(destinationMapper::toResponse)
//                .toList();
//    }
//
//    // Get popular destinations
//    @Cacheable(value = "popularDestinations", key = "#pageable.pageNumber")
//    public Page<DestinationResponse> getPopularDestinations(Pageable pageable) {
//        log.debug("Fetching popular destinations");
//
//        Page<Destination> destinations = destinationRepository
//                .findByPopularityScoreGreaterThanOrderByPopularityScoreDesc(50, pageable);
//
//        return destinations.map(destinationMapper::toResponse);
//    }
//
//    // Get top_rated destinations
//    @Cacheable(value = "topRatedDestinations", key = "#pageable.pageNumber")
//    public Page<DestinationResponse> getTopRatedDestinations(Pageable pageable) {
//        log.debug("Fetching top rated destinations");
//
//        Page<Destination> destinations = destinationRepository
//                .findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(
//                        new BigDecimal("4.0"), pageable);
//
//        return destinations.map(destinationMapper::toResponse);
//    }
//
//    // Get destinations by district
//    @Cacheable(value = "destinationsByDistrict", key = "#district + '-' + #pageable.pageNumber")
//    public Page<DestinationResponse> getDestinationsByDistrict(String district, Pageable pageable) {
//        log.debug("Fetching destinations in district: {}", district);
//
//        Page<Destination> destinations = destinationRepository.findByDistrict(district, pageable);
//        return destinations.map(destinationMapper::toResponse);
//    }
//}
