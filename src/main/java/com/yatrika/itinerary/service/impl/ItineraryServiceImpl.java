package com.yatrika.itinerary.service.impl;

import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.itinerary.domain.*;
import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.mapper.ItineraryMapper;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.itinerary.repository.SavedItineraryRepository;
import com.yatrika.itinerary.repository.UserLikedItineraryRepository;
import com.yatrika.itinerary.service.ItineraryService;
import com.yatrika.shared.exception.BadRequestException;
import com.yatrika.shared.exception.ForbiddenException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.shared.security.UserPrincipal;
import com.yatrika.shared.service.impl.CloudinaryStorageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryMapper itineraryMapper;
    private final DestinationRepository destinationRepository;
    private final SavedItineraryRepository savedItineraryRepository;
    private final UserLikedItineraryRepository userLikedItineraryRepository;
    private final CloudinaryStorageServiceImpl cloudinaryStorageService;

    // Helper method to get current user ID (returns null for guests)
    private Long getCurrentUserIdOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication.getPrincipal() instanceof String)) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                return principal.getId();
            }
            return null;
        } catch (Exception e) {
            log.debug("No authenticated user found, returning null");
            return null;
        }
    }

    // ================= DISCOVERY =================

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryResponse> getAdminTemplates() {
        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryRepository.findByStatusAndIsAdminCreatedTrue(ItineraryStatus.TEMPLATE)
                .stream()
                .map(itinerary -> itineraryMapper.toResponse(itinerary, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable) {
        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryRepository.findPublicCommunityTrips(
                        ItineraryStatus.COMPLETED, pageable)
                .map(itinerary -> itineraryMapper.toResponse(itinerary, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable) {
        Long currentUserId = getCurrentUserIdOrNull();
        Specification<Itinerary> spec = Specification.where(isPublicAndCompleted())
                .and(hasTheme(filter.getTheme()))
                .and(matchesSearch(filter.getSearchQuery()));
        return itineraryRepository.findAll(spec, pageable)
                .map(itinerary -> itineraryMapper.toResponse(itinerary, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryResponse getItineraryById(Long id, Long currentUserId) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));

        boolean isPublic = Boolean.TRUE.equals(itinerary.getIsPublic()) || itinerary.getIsAdminCreated();
        boolean isOwner = Objects.equals(itinerary.getUserId(), currentUserId);

        if (!isPublic && !isOwner) {
            throw new ForbiddenException("Access Denied: This itinerary is private");
        }

        return itineraryMapper.toResponse(itinerary, currentUserId);
    }

    // ================= PERSONAL MANAGEMENT =================

    @Override
    public ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        // Auto-calculate End Date if only Start and Duration are provided
        if (start != null && end == null && request.getTotalDays() != null) {
            end = start.plusDays(request.getTotalDays() - 1);
        }
        Itinerary trip = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .userId(userId)
                .status(ItineraryStatus.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(calculateDuration(request.getStartDate(), request.getEndDate())) // Auto-calculate
                .isAdminCreated(false)
                .isPublic(false)
                .images(new ArrayList<>())
                .build();
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable) {
        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(itinerary -> itineraryMapper.toResponse(itinerary, userId));
    }

    @Override
    public ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(id, userId);

        // 1. Basic Info
        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setTheme(request.getTheme());
        itinerary.setEstimatedBudget(request.getEstimatedBudget());

        // 2. Handle Day Changes (Add/Remove Day)
        if (request.getTotalDays() != null) {
            int oldTotalDays = itinerary.getTotalDays() != null ? itinerary.getTotalDays() : 0;
            int newTotalDays = request.getTotalDays();

            // Cleanup activities if reducing days
            if (newTotalDays < oldTotalDays) {
                itinerary.getItems().removeIf(item -> item.getDayNumber() > newTotalDays);
            }

            itinerary.setTotalDays(newTotalDays);

            // Sync End Date based on existing Start Date
            if (itinerary.getStartDate() != null) {
                itinerary.setEndDate(itinerary.getStartDate().plusDays(newTotalDays - 1));
            }
        }
        // 3. Handle Date Changes (If user manually changes dates in a picker)
        else if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new BadRequestException("End date cannot be before start date");
            }
            itinerary.setStartDate(request.getStartDate());
            itinerary.setEndDate(request.getEndDate());

            long days = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
            itinerary.setTotalDays((int) days);
        }

        // 4. Handle Images
        if (request.getImages() != null) {
            itinerary.setImages(new ArrayList<>(request.getImages()));
        }

        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }

    // ================= ITEM MANAGEMENT =================

    @Override
    public ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        validateDuplicate(itinerary, request);

        var destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId()));

        ItineraryItem item = ItineraryItem.builder()
                .itinerary(itinerary)
                .destination(destination)
                .title(request.getTitle())
                .notes(request.getNotes())
                .dayNumber(request.getDayNumber())
                .orderInDay(request.getOrderInDay())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .activityType(request.getActivityType())
                .isVisited(false)
                .build();
        itinerary.addItem(item);
        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }

    @Override
    public ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);

        ItineraryItem item = itinerary.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ItineraryItem", "id", itemId));

        validateDuplicate(itinerary, request, itemId);

        item.setTitle(request.getTitle());
        item.setNotes(request.getNotes());
        item.setDayNumber(request.getDayNumber());
        item.setOrderInDay(request.getOrderInDay());
        item.setStartTime(request.getStartTime());
        item.setEndTime(request.getEndTime());
        item.setActivityType(request.getActivityType());

        if (request.getDestinationId() != null) {
            item.setDestination(destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId())));
        }

        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }

    @Override
    public void removeItem(Long itineraryId, Long itemId, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        boolean removed = itinerary.getItems().removeIf(i -> i.getId().equals(itemId));
        if (removed) itineraryRepository.save(itinerary);
    }

    @Override
    public void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        itinerary.getItems().stream().filter(i -> i.getId().equals(itemId))
                .findFirst().ifPresent(item -> {
                    item.setIsVisited(visited);
                    itineraryRepository.save(itinerary);
                });
    }

    @Override
    public void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        for (int i = 0; i < itemIdsInOrder.size(); i++) {
            Long currentId = itemIdsInOrder.get(i);
            int newOrder = i + 1;
            itinerary.getItems().stream().filter(item -> item.getId().equals(currentId))
                    .findFirst().ifPresent(item -> item.setOrderInDay(newOrder));
        }
        itineraryRepository.save(itinerary);
    }

    // ================= COPY & SHARE =================

    @Override
    public ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId, LocalDate startDate) {
        Itinerary original = itineraryRepository.findById(targetItineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", targetItineraryId));

        if (original.getSourceId() != null)
            throw new BadRequestException("Cannot copy a copied itinerary");

        // Calculate End Date if Start Date is provided
        LocalDate endDate = null;
        if (startDate != null && original.getTotalDays() != null) {
            // If original is 3 days, and start is Monday, end is Wednesday (Monday + 2 days)
            endDate = startDate.plusDays(original.getTotalDays() - 1);
        }

        Itinerary clone = Itinerary.builder()
                .title(original.getTitle())
                .description(original.getDescription())
                .theme(original.getTheme())
                .userId(currentUserId)
                .status(ItineraryStatus.DRAFT)
                .isAdminCreated(false)
                .isPublic(false)
                .sourceId(original.getId())
                .startDate(startDate) // The new start date
                .endDate(endDate)     // Automatically calculated end date
                .totalDays(original.getTotalDays())
                .estimatedBudget(original.getEstimatedBudget())
                .images(new ArrayList<>(original.getImages()))
                .items(new ArrayList<>())
                .build();

        // Clone items
        original.getItems().forEach(item -> {
            ItineraryItem newItem = ItineraryItem.builder()
                    .itinerary(clone)
                    .destination(item.getDestination())
                    .title(item.getTitle())
                    .notes(item.getNotes())
                    .dayNumber(item.getDayNumber())
                    .orderInDay(item.getOrderInDay())
                    .startTime(item.getStartTime())
                    .endTime(item.getEndTime())
                    .activityType(item.getActivityType())
                    .isVisited(false)
                    .build();
            clone.addItem(newItem);
        });

        original.setCopyCount(original.getCopyCount() + 1);
        itineraryRepository.save(original);

        return itineraryMapper.toResponse(itineraryRepository.save(clone), currentUserId);
    }

    @Override
    public ItineraryResponse shareTrip(Long id, Long userId) {
        Itinerary trip = getOwnedItinerary(id, userId);

        if (trip.getSourceId() != null) {
            throw new BadRequestException("Copied trips cannot be shared");
        }

        trip.setIsPublic(true);
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    public ItineraryResponse unshareTrip(Long id, Long userId) {
        Itinerary trip = getOwnedItinerary(id, userId);

        if (!Boolean.TRUE.equals(trip.getIsPublic())) {
            throw new BadRequestException("Trip is already private");
        }

        trip.setIsPublic(false);
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    public ItineraryResponse completeTrip(Long itineraryId, Long userId) {
        Itinerary trip = getOwnedItinerary(itineraryId, userId);

        if (trip.getItems().isEmpty())
            throw new BadRequestException("Cannot complete empty trip");

        trip.setStatus(ItineraryStatus.COMPLETED);
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    public ItineraryResponse uploadItineraryImages(Long id, List<MultipartFile> files, Long userId) {
        Itinerary itinerary = getOwnedItinerary(id, userId);

        if (files != null && !files.isEmpty()) {
            List<String> uploadedUrls = cloudinaryStorageService.uploadMultipleFiles(files, "admin_templates");
            itinerary.getImages().addAll(uploadedUrls);
        }

        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }

    @Override
    @Transactional
    public void deleteItinerary(Long id, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));

        if (!itinerary.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to delete this itinerary");
        }

        itineraryRepository.delete(itinerary);
    }

    // ================= ADMIN =================

    @Override
    public ItineraryResponse createAdminTemplate(ItineraryRequest request) {
        Itinerary template = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .status(ItineraryStatus.TEMPLATE)
                .isAdminCreated(true)
                .isPublic(true)
                .totalDays(request.getTotalDays())
                .images(request.getImages() != null ? new ArrayList<>(request.getImages()) : new ArrayList<>())
                .build();
        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
    }

    @Override
    public ItineraryResponse createAdminTemplate(
            ItineraryRequest request,
            List<MultipartFile> files
    ) {
        // 1. Create template FIRST (no images yet)
        Itinerary template = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .status(ItineraryStatus.TEMPLATE)
                .isAdminCreated(true)
                .isPublic(true)
                .totalDays(request.getTotalDays())
                .estimatedBudget(request.getEstimatedBudget())
                .images(new ArrayList<>())
                .build();

        itineraryRepository.save(template); // ID generated here ✅

        // 2. Upload images (optional)
        if (files != null && !files.isEmpty()) {
            List<String> uploadedUrls = cloudinaryStorageService.uploadMultipleFiles(files, "admin_templates");
            template.getImages().addAll(uploadedUrls);
        }

        // 3. Save updated template
        itineraryRepository.save(template);

        return itineraryMapper.toResponse(template, getCurrentUserIdOrNull());
    }


    @Override
    public ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));

        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setTheme(request.getTheme());
        template.setTotalDays(request.getTotalDays());

        if (request.getImages() != null) {
            template.setImages(new ArrayList<>(request.getImages()));
        }

        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
    }

    @Override
    public void removeAdminTemplate(Long templateId) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));

        itineraryRepository.delete(template);
    }

    @Override
    public ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));

        validateDuplicate(template, request);

        var destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId()));

        ItineraryItem item = ItineraryItem.builder()
                .itinerary(template)
                .destination(destination)
                .title(request.getTitle())
                .dayNumber(request.getDayNumber())
                .orderInDay(request.getOrderInDay())
                .activityType(request.getActivityType())
                .build();

        template.addItem(item);
        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
    }

    @Override
    public ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));

        ItineraryItem item = template.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ItineraryItem", "id", itemId));

        validateDuplicate(template, request, itemId);

        item.setTitle(request.getTitle());
        item.setDayNumber(request.getDayNumber());
        item.setOrderInDay(request.getOrderInDay());
        item.setActivityType(request.getActivityType());

        if (request.getDestinationId() != null) {
            item.setDestination(destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId())));
        }

        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
    }

    @Override
    public void removeItemFromTemplate(Long templateId, Long itemId) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));

        boolean removed = template.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) throw new ResourceNotFoundException("ItineraryItem", "id", itemId);

        itineraryRepository.save(template);
    }

    @Override
    public ItineraryResponse uploadAdminTemplateImages(
            Long templateId,
            List<MultipartFile> files
    ) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Admin Template", "id", templateId)
                );

        if (files != null && !files.isEmpty()) {
            List<String> uploadedUrls = cloudinaryStorageService.uploadMultipleFiles(files, "admin_templates");
            template.getImages().addAll(uploadedUrls);
        }

        return itineraryMapper.toResponse(
                itineraryRepository.save(template),
                getCurrentUserIdOrNull()
        );
    }

    // ================= Save and Unsaved ================
    @Override
    public ItineraryResponse saveItinerary(Long itineraryId, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        if (savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId)) {
            throw new BadRequestException("Itinerary already saved");
        }

        SavedItinerary saved = SavedItinerary.builder()
                .itinerary(itinerary)
                .userId(userId)
                .savedAt(LocalDateTime.now())
                .build();

        savedItineraryRepository.save(saved);
        return itineraryMapper.toResponse(itinerary, userId);
    }

    @Override
    public ItineraryResponse unsaveItinerary(Long itineraryId, Long userId) {
        SavedItinerary saved = savedItineraryRepository
                .findByItineraryIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedItinerary", "userId", userId));

        savedItineraryRepository.delete(saved);

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        return itineraryMapper.toResponse(itinerary, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryResponse> getMySavedItineraries(Long userId) {
        return savedItineraryRepository.findByUserId(userId).stream()
                .map(saved -> itineraryMapper.toResponse(saved.getItinerary(), userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isItinerarySaved(Long itineraryId, Long userId) {
        return savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);
    }


    // ==================== Like Toggle =============================
    @Override
    public ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        // Store the itinerary in a final variable for lambda
        final Itinerary finalItinerary = itinerary;

        userLikedItineraryRepository.findByItineraryIdAndUserId(itineraryId, userId)
                .ifPresentOrElse(
                        like -> {
                            userLikedItineraryRepository.delete(like);
                            // Use finalItinerary instead of itinerary
                            finalItinerary.setLikeCount(Math.max(0, finalItinerary.getLikeCount() - 1));
                        },
                        () -> {
                            userLikedItineraryRepository.save(UserLikedItinerary.builder()
                                    .itinerary(finalItinerary)
                                    .userId(userId)
                                    .likedAt(LocalDateTime.now())
                                    .build());
                            // Use finalItinerary instead of itinerary
                            finalItinerary.setLikeCount(finalItinerary.getLikeCount() + 1);
                        }
                );

        // Save the itinerary
        itinerary = itineraryRepository.save(finalItinerary);

        // Get the ACTUAL like count from database - convert Long to Integer
        Long actualLikeCountLong = userLikedItineraryRepository.countByItineraryId(itineraryId);
        Integer actualLikeCount = actualLikeCountLong != null ? actualLikeCountLong.intValue() : 0;
        itinerary.setLikeCount(actualLikeCount);

        return itineraryMapper.toResponse(itinerary, userId);
    }

    // ================= PRIVATE HELPERS =================

    private Integer calculateDuration(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return null; // Allows for "Dream Trips"
        }
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date.");
        }
        // +1 because a trip from Monday to Monday is 1 day, but usually users mean 1 day of activities
        // ChronoUnit.DAYS.between(start, end) is standard, but check if your UI expects (days + 1)
        return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
    }

    private Itinerary getOwnedItinerary(Long itineraryId, Long userId) {
        return itineraryRepository.findById(itineraryId)
                .filter(it -> Objects.equals(it.getUserId(), userId))
                .orElseThrow(() -> new ForbiddenException("You do not have permission to access this itinerary"));
    }

    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request) {
        validateDuplicate(itinerary, request, null);
    }

    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request, Long ignoreItemId) {
        boolean exists = itinerary.getItems().stream()
                .anyMatch(i -> !Objects.equals(i.getId(), ignoreItemId)
                        && i.getDestination().getId().equals(request.getDestinationId())
                        && i.getDayNumber().equals(request.getDayNumber()));
        if (exists) throw new BadRequestException("This destination is already planned for this day.");
    }

    private void validateDuplicateInRequest(List<ItineraryItemRequest> items) {
        long uniqueCount = items.stream()
                .map(i -> i.getDestinationId() + "-" + i.getDayNumber())
                .distinct().count();
        if (uniqueCount != items.size()) throw new BadRequestException("Duplicate destinations found in your plan.");
    }

    private Specification<Itinerary> isPublicAndCompleted() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isPublic"), true),
                cb.equal(root.get("status"), ItineraryStatus.COMPLETED),
                cb.equal(root.get("isAdminCreated"), false),
                cb.isNull(root.get("sourceId"))
        );
    }

    private Specification<Itinerary> hasTheme(String theme) {
        return (theme == null || theme.isBlank()) ? null : (root, query, cb) -> cb.equal(root.get("theme"), theme);
    }

    private Specification<Itinerary> matchesSearch(String search) {
        return (search == null || search.isBlank()) ? null : (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
    }
}