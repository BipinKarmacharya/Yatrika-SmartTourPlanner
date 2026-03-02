package com.yatrika.itinerary.service.impl;

import com.yatrika.destination.domain.Destination;
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

    // =========================================================================
    // INTERNAL HELPERS – shared logic & permission checks
    // =========================================================================

    private Long getCurrentUserIdOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
                return principal.getId();
            }
        } catch (Exception e) {
            log.debug("Guest user context");
        }
        return null;
    }

    /**
     * Fetch an itinerary for update operations.
     * @param id itinerary id
     * @param userId current user id (null if not applicable, e.g., admin flow)
     * @param isAdminFlow if true, ensures the itinerary is an admin template; otherwise checks ownership
     * @return the itinerary
     */
    private Itinerary getItineraryForUpdate(Long id, Long userId, boolean isAdminFlow) {
        Itinerary it = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));

        if (isAdminFlow) {
            if (!it.getIsAdminCreated() || it.getStatus() != ItineraryStatus.TEMPLATE) {
                throw new BadRequestException("Target is not a manageable admin template");
            }
        } else {
            if (!Objects.equals(it.getUserId(), userId)) {
                throw new ForbiddenException("You do not own this itinerary");
            }
        }
        return it;
    }

    /**
     * Check if a user can view an itinerary (public, admin‑created, or owner).
     */
    private boolean canAccess(Itinerary itinerary, Long userId) {
        return Boolean.TRUE.equals(itinerary.getIsPublic()) ||
                itinerary.getIsAdminCreated() ||
                Objects.equals(itinerary.getUserId(), userId);
    }

    /**
     * Validate that the same destination is not already added on the same day.
     */
    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request, Long ignoreItemId) {
        if (itinerary.getItems() == null) return;

        boolean exists = itinerary.getItems().stream()
                .filter(i -> !Objects.equals(i.getId(), ignoreItemId))
                .anyMatch(i -> i.getDestination() != null &&
                        i.getDestination().getId().equals(request.getDestinationId()) &&
                        i.getDayNumber().equals(request.getDayNumber()));

        if (exists) {
            throw new BadRequestException("Destination already exists for Day " + request.getDayNumber());
        }
    }

    /**
     * Calculate number of days between start and end (inclusive).
     */
    private Integer calculateDuration(LocalDate start, LocalDate end) {
        if (start == null || end == null) return null;
        if (end.isBefore(start)) throw new BadRequestException("End date cannot be before start date.");
        return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
    }

    // =========================================================================
    // INTERNAL OPERATIONS (shared between user & admin flows)
    // =========================================================================

    private Itinerary internalAddItem(Itinerary itinerary, ItineraryItemRequest request) {
        validateDuplicate(itinerary, request, null);

        Destination dest = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId()));

        ItineraryItem item = ItineraryItem.builder()
                .itinerary(itinerary)
                .destination(dest)
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
        return itineraryRepository.save(itinerary);
    }

    private Itinerary internalUpdateItem(Itinerary itinerary, Long itemId, ItineraryItemRequest request) {
        ItineraryItem item = itinerary.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        // Fill missing fields for validation
        if (request.getDestinationId() == null) request.setDestinationId(item.getDestination().getId());
        if (request.getDayNumber() == null) request.setDayNumber(item.getDayNumber());

        validateDuplicate(itinerary, request, itemId);

        // Apply updates
        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getNotes() != null) item.setNotes(request.getNotes());
        if (request.getDayNumber() != null) item.setDayNumber(request.getDayNumber());
        if (request.getOrderInDay() != null) item.setOrderInDay(request.getOrderInDay());
        if (request.getStartTime() != null) item.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) item.setEndTime(request.getEndTime());
        if (request.getActivityType() != null) item.setActivityType(request.getActivityType());

        if (!item.getDestination().getId().equals(request.getDestinationId())) {
            item.setDestination(destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId())));
        }

        return itineraryRepository.save(itinerary);
    }

    private void internalRemoveItem(Itinerary itinerary, Long itemId) {
        boolean removed = itinerary.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) throw new ResourceNotFoundException("Item", "id", itemId);
        itineraryRepository.save(itinerary);
    }

    private void internalReorderItems(Itinerary itinerary, List<Long> itemIdsInOrder) {
        for (int i = 0; i < itemIdsInOrder.size(); i++) {
            Long currentId = itemIdsInOrder.get(i);
            final int newOrder = i + 1;               // effectively final copy
            itinerary.getItems().stream()
                    .filter(item -> item.getId().equals(currentId))
                    .findFirst()
                    .ifPresent(item -> item.setOrderInDay(newOrder));
        }
        itineraryRepository.save(itinerary);
    }

    private void internalToggleItemVisited(Itinerary itinerary, Long itemId, Boolean visited) {
        itinerary.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> {
                    item.setIsVisited(visited);
                    itineraryRepository.save(itinerary);
                });
    }

    private Itinerary internalUploadImages(Itinerary itinerary, List<MultipartFile> files) {
        if (files != null && !files.isEmpty()) {
            List<String> urls = cloudinaryStorageService.uploadMultipleFiles(files, "itineraries");
            int order = itinerary.getImages().size();
            for (String url : urls) {
                itinerary.addImage(ItineraryImage.builder()
                        .url(url)
                        .sortOrder(order++)
                        .isCover(false)
                        .build());
            }
        }
        return itineraryRepository.save(itinerary);
    }

    private Itinerary internalRemoveImage(Itinerary itinerary, Long imageId) {
        ItineraryImage image = itinerary.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ItineraryImage", "id", imageId));

        boolean wasCover = Boolean.TRUE.equals(image.getIsCover());
        itinerary.removeImage(image);

        if (wasCover && !itinerary.getImages().isEmpty()) {
            itinerary.getImages().get(0).setIsCover(true);
        }
        return itineraryRepository.save(itinerary);
    }

    private Itinerary internalSetCoverImage(Itinerary itinerary, Long imageId) {
        ItineraryImage image = itinerary.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ItineraryImage", "id", imageId));

        // Unset any existing cover
        itinerary.getImages().forEach(img -> img.setIsCover(false));
        image.setIsCover(true);
        return itineraryRepository.save(itinerary);
    }

    private void internalUpdateDatesAndDuration(Itinerary itinerary, ItineraryRequest request) {
        if (request.getTotalDays() != null) {
            int newDays = request.getTotalDays();
            // Remove items outside the new duration
            itinerary.getItems().removeIf(item -> item.getDayNumber() > newDays);
            itinerary.setTotalDays(newDays);
            if (itinerary.getStartDate() != null) {
                itinerary.setEndDate(itinerary.getStartDate().plusDays(newDays - 1));
            }
        } else if (request.getStartDate() != null && request.getEndDate() != null) {
            itinerary.setStartDate(request.getStartDate());
            itinerary.setEndDate(request.getEndDate());
            itinerary.setTotalDays(calculateDuration(request.getStartDate(), request.getEndDate()));
        }
    }

    private void internalSyncHeaderImages(Itinerary itinerary, ItineraryRequest request) {
        if (request.getImages() != null) {
            itinerary.getImages().clear();
            request.getImages().forEach(imgReq -> itinerary.addImage(
                    ItineraryImage.builder()
                            .url(imgReq.getUrl())
                            .sortOrder(imgReq.getSortOrder())
                            .isCover(Boolean.TRUE.equals(imgReq.getIsCover()))
                            .build()
            ));
        }
    }

    // =========================================================================
    // DISCOVERY
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryResponse> getAdminTemplates() {
        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryRepository.findByStatusAndIsAdminCreatedTrue(ItineraryStatus.TEMPLATE)
                .stream()
                .map(it -> itineraryMapper.toResponse(it, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable) {
        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryRepository.findPublicCommunityTrips(ItineraryStatus.COMPLETED, pageable)
                .map(it -> itineraryMapper.toResponse(it, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable) {
        Long currentUserId = getCurrentUserIdOrNull();
        Specification<Itinerary> spec = Specification.where(isPublicAndCompleted())
                .and(hasTheme(filter.getTheme()))
                .and(matchesSearch(filter.getSearchQuery()));
        return itineraryRepository.findAll(spec, pageable)
                .map(it -> itineraryMapper.toResponse(it, currentUserId));
    }

    private Specification<Itinerary> isPublicAndCompleted() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("isPublic")),
                cb.equal(root.get("status"), ItineraryStatus.COMPLETED),
                cb.isFalse(root.get("isAdminCreated")),
                cb.isNull(root.get("sourceId"))
        );
    }

    private Specification<Itinerary> hasTheme(String theme) {
        return (theme == null || theme.isBlank()) ? null :
                (root, query, cb) -> cb.equal(root.get("theme"), theme);
    }

    private Specification<Itinerary> matchesSearch(String search) {
        return (search == null || search.isBlank()) ? null :
                (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryResponse getItineraryById(Long id, Long currentUserId) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));

        if (!canAccess(itinerary, currentUserId)) {
            throw new ForbiddenException("Access Denied: This itinerary is private");
        }
        return itineraryMapper.toResponse(itinerary, currentUserId);
    }

    // =========================================================================
    // PERSONAL MANAGEMENT
    // =========================================================================

    @Override
    public ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (start != null && end == null && request.getTotalDays() != null) {
            end = start.plusDays(request.getTotalDays() - 1);
        }

        Itinerary trip = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .userId(userId)
                .status(ItineraryStatus.DRAFT)
                .startDate(start)
                .endDate(end)
                .totalDays(calculateDuration(start, end))
                .isAdminCreated(false)
                .isPublic(false)
                .images(new ArrayList<>())
                .items(new ArrayList<>())
                .build();

        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    public ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(id, userId, false);

        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setTheme(request.getTheme());
        itinerary.setEstimatedBudget(request.getEstimatedBudget());

        internalUpdateDatesAndDuration(itinerary, request);
        internalSyncHeaderImages(itinerary, request);

        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable) {
        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(it -> itineraryMapper.toResponse(it, userId));
    }

    @Override
    public ItineraryResponse completeTrip(Long itineraryId, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(itineraryId, userId, false);
        if (itinerary.getItems().isEmpty()) {
            throw new BadRequestException("Cannot complete an empty trip");
        }
        itinerary.setStatus(ItineraryStatus.COMPLETED);
        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }

    @Override
    public ItineraryResponse uploadItineraryImages(Long itineraryId, List<MultipartFile> files, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(itineraryId, userId, false);
        return itineraryMapper.toResponse(internalUploadImages(itinerary, files), userId);
    }

    @Override
    public void deleteItinerary(Long id, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(id, userId, false);
        itineraryRepository.delete(itinerary);
    }

    @Override
    public void deleteAllMyItineraries(Long userId) {
        List<Itinerary> userItineraries = itineraryRepository.findByUserId(userId);
        if (!userItineraries.isEmpty()) {
            itineraryRepository.deleteAll(userItineraries);
        }
    }

    // =========================================================================
    // COPY & SHARE
    // =========================================================================

    @Override
    public ItineraryResponse copyItinerary(Long targetId, Long userId, LocalDate newStart) {
        Itinerary source = itineraryRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", targetId));

        if (source.getSourceId() != null) {
            throw new BadRequestException("Cannot copy an already copied itinerary.");
        }

        LocalDate newEnd = (newStart != null && source.getTotalDays() != null)
                ? newStart.plusDays(source.getTotalDays() - 1) : null;

        Itinerary clone = Itinerary.builder()
                .title(source.getTitle() + " (Copy)")
                .description(source.getDescription())
                .userId(userId)
                .status(ItineraryStatus.DRAFT)
                .sourceId(source.getId())
                .startDate(newStart)
                .endDate(newEnd)
                .totalDays(source.getTotalDays())
                .images(new ArrayList<>())
                .items(new ArrayList<>())
                .build();

        // Deep copy images
        source.getImages().forEach(img -> clone.addImage(
                ItineraryImage.builder()
                        .url(img.getUrl())
                        .sortOrder(img.getSortOrder())
                        .isCover(img.getIsCover())
                        .build()
        ));

        // Deep copy items
        source.getItems().forEach(item -> clone.addItem(
                ItineraryItem.builder()
                        .destination(item.getDestination())
                        .title(item.getTitle())
                        .notes(item.getNotes())
                        .dayNumber(item.getDayNumber())
                        .orderInDay(item.getOrderInDay())
                        .activityType(item.getActivityType())
                        .isVisited(false)
                        .build()
        ));

        source.setCopyCount(source.getCopyCount() + 1);
        itineraryRepository.save(source);

        return itineraryMapper.toResponse(itineraryRepository.save(clone), userId);
    }

    @Override
    public ItineraryResponse shareTrip(Long itineraryId, Long userId) {
        Itinerary trip = getItineraryForUpdate(itineraryId, userId, false);
        if (trip.getSourceId() != null) {
            throw new BadRequestException("Copied trips cannot be shared");
        }
        trip.setIsPublic(true);
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    public ItineraryResponse unshareTrip(Long itineraryId, Long userId) {
        Itinerary trip = getItineraryForUpdate(itineraryId, userId, false);
        if (!Boolean.TRUE.equals(trip.getIsPublic())) {
            throw new BadRequestException("Trip is already private");
        }
        trip.setIsPublic(false);
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    // =========================================================================
    // ITEM MANAGEMENT (User)
    // =========================================================================

    @Override
    public ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(itineraryId, userId, false);
        return itineraryMapper.toResponse(internalAddItem(itinerary, request), userId);
    }

    @Override
    public ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(itineraryId, userId, false);
        return itineraryMapper.toResponse(internalUpdateItem(itinerary, itemId, request), userId);
    }

    @Override
    public void removeItem(Long itineraryId, Long itemId, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(itineraryId, userId, false);
        internalRemoveItem(itinerary, itemId);
    }

    @Override
    public void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(itineraryId, userId, false);
        internalToggleItemVisited(itinerary, itemId, visited);
    }

    @Override
    public void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId) {
        Itinerary itinerary = getItineraryForUpdate(itineraryId, userId, false);
        internalReorderItems(itinerary, itemIdsInOrder);
    }

    // =========================================================================
    // ADMIN TEMPLATES
    // =========================================================================

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
                .estimatedBudget(request.getEstimatedBudget())
                .images(new ArrayList<>())
                .build();

        if (request.getImages() != null) {
            request.getImages().forEach(imgReq -> template.addImage(
                    ItineraryImage.builder()
                            .url(imgReq.getUrl())
                            .sortOrder(imgReq.getSortOrder())
                            .isCover(Boolean.TRUE.equals(imgReq.getIsCover()))
                            .build()
            ));
        }

        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
    }

    @Override
    public ItineraryResponse createAdminTemplate(ItineraryRequest request, List<MultipartFile> files) {
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

        itineraryRepository.save(template); // save first to get id for image association if needed

        if (files != null && !files.isEmpty()) {
            List<String> urls = cloudinaryStorageService.uploadMultipleFiles(files, "admin_templates");
            int order = 0;
            for (String url : urls) {
                template.addImage(ItineraryImage.builder()
                        .url(url)
                        .sortOrder(order++)
                        .isCover(order == 1) // first uploaded becomes cover? adjust as needed
                        .build());
            }
        }

        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
    }

    @Override
    public ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);

        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setTheme(request.getTheme());
        template.setTotalDays(request.getTotalDays());

        internalSyncHeaderImages(template, request);

        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
    }

    @Override
    public void removeAdminTemplate(Long templateId) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);
        itineraryRepository.delete(template);
    }

    @Override
    public ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest request) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);
        return itineraryMapper.toResponse(internalAddItem(template, request), getCurrentUserIdOrNull());
    }

    @Override
    public ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest request) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);
        return itineraryMapper.toResponse(internalUpdateItem(template, itemId, request), getCurrentUserIdOrNull());
    }

    @Override
    public void removeItemFromTemplate(Long templateId, Long itemId) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);
        internalRemoveItem(template, itemId);
    }

    @Override
    public ItineraryResponse uploadAdminTemplateImages(Long templateId, List<MultipartFile> files) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);
        return itineraryMapper.toResponse(internalUploadImages(template, files), getCurrentUserIdOrNull());
    }

    @Override
    public ItineraryResponse removeAdminTemplateImage(Long templateId, Long imageId) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);
        return itineraryMapper.toResponse(internalRemoveImage(template, imageId), getCurrentUserIdOrNull());
    }

    @Override
    public ItineraryResponse setAdminTemplateCoverImage(Long templateId, Long imageId) {
        Itinerary template = getItineraryForUpdate(templateId, null, true);
        return itineraryMapper.toResponse(internalSetCoverImage(template, imageId), getCurrentUserIdOrNull());
    }

    // =========================================================================
    // SOCIAL (SAVE / LIKE)
    // =========================================================================

    @Override
    @Transactional
    public ItineraryResponse saveItinerary(Long itineraryId, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        if (savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId)) {
            throw new BadRequestException("Already saved.");
        }

        savedItineraryRepository.save(SavedItinerary.builder()
                .itinerary(itinerary)
                .userId(userId)
                .savedAt(LocalDateTime.now())
                .build());

        return itineraryMapper.toResponse(itinerary, userId);
    }

    @Override
    @Transactional
    public ItineraryResponse unsaveItinerary(Long itineraryId, Long userId) {
        if (!savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId)) {
            throw new ResourceNotFoundException("SavedItinerary", "itineraryId", itineraryId);
        }
        savedItineraryRepository.deleteByItineraryIdAndUserId(itineraryId, userId);
        savedItineraryRepository.flush(); // ensure consistency for subsequent reads

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

    @Override
    public ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        userLikedItineraryRepository.findByItineraryIdAndUserId(itineraryId, userId)
                .ifPresentOrElse(
                        like -> {
                            userLikedItineraryRepository.delete(like);
                            itinerary.setLikeCount(Math.max(0, itinerary.getLikeCount() - 1));
                        },
                        () -> {
                            userLikedItineraryRepository.save(UserLikedItinerary.builder()
                                    .itinerary(itinerary)
                                    .userId(userId)
                                    .likedAt(LocalDateTime.now())
                                    .build());
                            itinerary.setLikeCount(itinerary.getLikeCount() + 1);
                        }
                );

        // Sync with actual count from DB (optional, but safe)
        Long actualCount = userLikedItineraryRepository.countByItineraryId(itineraryId);
        itinerary.setLikeCount(actualCount != null ? actualCount.intValue() : 0);

        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }
}










//package com.yatrika.itinerary.service.impl;
//
//import com.yatrika.destination.repository.DestinationRepository;
//import com.yatrika.itinerary.domain.*;
//import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
//import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
//import com.yatrika.itinerary.dto.request.ItineraryRequest;
//import com.yatrika.itinerary.dto.response.ItineraryResponse;
//import com.yatrika.itinerary.mapper.ItineraryMapper;
//import com.yatrika.itinerary.repository.ItineraryRepository;
//import com.yatrika.itinerary.repository.SavedItineraryRepository;
//import com.yatrika.itinerary.repository.UserLikedItineraryRepository;
//import com.yatrika.itinerary.service.ItineraryService;
//import com.yatrika.shared.exception.BadRequestException;
//import com.yatrika.shared.exception.ForbiddenException;
//import com.yatrika.shared.exception.ResourceNotFoundException;
//import com.yatrika.shared.security.UserPrincipal;
//import com.yatrika.shared.service.impl.CloudinaryStorageServiceImpl;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class ItineraryServiceImpl implements ItineraryService {
//
//    private final ItineraryRepository itineraryRepository;
//    private final ItineraryMapper itineraryMapper;
//    private final DestinationRepository destinationRepository;
//    private final SavedItineraryRepository savedItineraryRepository;
//    private final UserLikedItineraryRepository userLikedItineraryRepository;
//    private final CloudinaryStorageServiceImpl cloudinaryStorageService;
//
//    // Helper method to get current user ID (returns null for guests)
//    private Long getCurrentUserIdOrNull() {
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            if (authentication != null && authentication.isAuthenticated()
//                    && !(authentication.getPrincipal() instanceof String)) {
//                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
//                return principal.getId();
//            }
//            return null;
//        } catch (Exception e) {
//            log.debug("No authenticated user found, returning null");
//            return null;
//        }
//    }
//
//    // ================= DISCOVERY =================
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ItineraryResponse> getAdminTemplates() {
//        Long currentUserId = getCurrentUserIdOrNull();
//        return itineraryRepository.findByStatusAndIsAdminCreatedTrue(ItineraryStatus.TEMPLATE)
//                .stream()
//                .map(itinerary -> itineraryMapper.toResponse(itinerary, currentUserId))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable) {
//        Long currentUserId = getCurrentUserIdOrNull();
//        return itineraryRepository.findPublicCommunityTrips(
//                        ItineraryStatus.COMPLETED, pageable)
//                .map(itinerary -> itineraryMapper.toResponse(itinerary, currentUserId));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable) {
//        Long currentUserId = getCurrentUserIdOrNull();
//        Specification<Itinerary> spec = Specification.where(isPublicAndCompleted())
//                .and(hasTheme(filter.getTheme()))
//                .and(matchesSearch(filter.getSearchQuery()));
//        return itineraryRepository.findAll(spec, pageable)
//                .map(itinerary -> itineraryMapper.toResponse(itinerary, currentUserId));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ItineraryResponse getItineraryById(Long id, Long currentUserId) {
//        Itinerary itinerary = itineraryRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));
//
//        boolean isPublic = Boolean.TRUE.equals(itinerary.getIsPublic()) || itinerary.getIsAdminCreated();
//        boolean isOwner = Objects.equals(itinerary.getUserId(), currentUserId);
//
//        if (!isPublic && !isOwner) {
//            throw new ForbiddenException("Access Denied: This itinerary is private");
//        }
//
//        return itineraryMapper.toResponse(itinerary, currentUserId);
//    }
//
//    // ================= PERSONAL MANAGEMENT =================
//
//    @Override
//    public ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId) {
//        LocalDate start = request.getStartDate();
//        LocalDate end = request.getEndDate();
//
//        // Auto-calculate End Date if only Start and Duration are provided
//        if (start != null && end == null && request.getTotalDays() != null) {
//            end = start.plusDays(request.getTotalDays() - 1);
//        }
//        Itinerary trip = Itinerary.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .theme(request.getTheme())
//                .userId(userId)
//                .status(ItineraryStatus.DRAFT)
//                .startDate(request.getStartDate())
//                .endDate(request.getEndDate())
//                .totalDays(calculateDuration(request.getStartDate(), request.getEndDate())) // Auto-calculate
//                .isAdminCreated(false)
//                .isPublic(false)
//                .images(new ArrayList<>())
//                .build();
//        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable) {
//        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
//                .map(itinerary -> itineraryMapper.toResponse(itinerary, userId));
//    }
//
//    @Override
//    public ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(id, userId);
//
//        itinerary.setTitle(request.getTitle());
//        itinerary.setDescription(request.getDescription());
//        itinerary.setTheme(request.getTheme());
//        itinerary.setEstimatedBudget(request.getEstimatedBudget());
//
//        // Handle days
//        if (request.getTotalDays() != null) {
//            int oldTotalDays = itinerary.getTotalDays() != null ? itinerary.getTotalDays() : 0;
//            int newTotalDays = request.getTotalDays();
//
//            if (newTotalDays < oldTotalDays) {
//                itinerary.getItems().removeIf(item -> item.getDayNumber() > newTotalDays);
//            }
//
//            itinerary.setTotalDays(newTotalDays);
//            if (itinerary.getStartDate() != null) {
//                itinerary.setEndDate(itinerary.getStartDate().plusDays(newTotalDays - 1));
//            }
//        } else if (request.getStartDate() != null && request.getEndDate() != null) {
//            if (request.getEndDate().isBefore(request.getStartDate())) {
//                throw new BadRequestException("End date cannot be before start date");
//            }
//            itinerary.setStartDate(request.getStartDate());
//            itinerary.setEndDate(request.getEndDate());
//            itinerary.setTotalDays(
//                    (int) java.time.temporal.ChronoUnit.DAYS
//                            .between(request.getStartDate(), request.getEndDate()) + 1
//            );
//        }
//
//        // ✅ FIXED IMAGE UPDATE
//        if (request.getImages() != null) {
//            itinerary.getImages().clear(); // orphanRemoval=true
//
//            request.getImages().forEach(imgReq -> {
//                ItineraryImage image = ItineraryImage.builder()
//                        .url(imgReq.getUrl())
//                        .sortOrder(imgReq.getSortOrder())
//                        .isCover(Boolean.TRUE.equals(imgReq.getIsCover()))
//                        .build();
//                itinerary.addImage(image);
//            });
//        }
//
//        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
//    }
//
//
//    // ================= ITEM MANAGEMENT =================
//
//    @Override
//    public ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest request, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
//        validateDuplicate(itinerary, request);
//
//        var destination = destinationRepository.findById(request.getDestinationId())
//                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId()));
//
//        ItineraryItem item = ItineraryItem.builder()
//                .itinerary(itinerary)
//                .destination(destination)
//                .title(request.getTitle())
//                .notes(request.getNotes())
//                .dayNumber(request.getDayNumber())
//                .orderInDay(request.getOrderInDay())
//                .startTime(request.getStartTime())
//                .endTime(request.getEndTime())
//                .activityType(request.getActivityType())
//                .isVisited(false)
//                .build();
//        itinerary.addItem(item);
//        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
//    }
//
//    @Override
//    public ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
//
//        ItineraryItem item = itinerary.getItems().stream()
//                .filter(i -> i.getId().equals(itemId))
//                .findFirst()
//                .orElseThrow(() -> new ResourceNotFoundException("ItineraryItem", "id", itemId));
//
//        validateDuplicate(itinerary, request, itemId);
//
//        item.setTitle(request.getTitle());
//        item.setNotes(request.getNotes());
//        item.setDayNumber(request.getDayNumber());
//        item.setOrderInDay(request.getOrderInDay());
//        item.setStartTime(request.getStartTime());
//        item.setEndTime(request.getEndTime());
//        item.setActivityType(request.getActivityType());
//
//        if (request.getDestinationId() != null) {
//            item.setDestination(destinationRepository.findById(request.getDestinationId())
//                    .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId())));
//        }
//
//        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
//    }
//
//    @Override
//    public void removeItem(Long itineraryId, Long itemId, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
//        boolean removed = itinerary.getItems().removeIf(i -> i.getId().equals(itemId));
//        if (removed) itineraryRepository.save(itinerary);
//    }
//
//    @Override
//    public void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
//        itinerary.getItems().stream().filter(i -> i.getId().equals(itemId))
//                .findFirst().ifPresent(item -> {
//                    item.setIsVisited(visited);
//                    itineraryRepository.save(itinerary);
//                });
//    }
//
//    @Override
//    public void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
//        for (int i = 0; i < itemIdsInOrder.size(); i++) {
//            Long currentId = itemIdsInOrder.get(i);
//            int newOrder = i + 1;
//            itinerary.getItems().stream().filter(item -> item.getId().equals(currentId))
//                    .findFirst().ifPresent(item -> item.setOrderInDay(newOrder));
//        }
//        itineraryRepository.save(itinerary);
//    }
//
//    // ================= COPY & SHARE =================
//
//    @Override
//    public ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId, LocalDate startDate) {
//        Itinerary original = itineraryRepository.findById(targetItineraryId)
//                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", targetItineraryId));
//
//        if (original.getSourceId() != null) {
//            throw new BadRequestException("Cannot copy a copied itinerary");
//        }
//
//        LocalDate endDate = null;
//        if (startDate != null && original.getTotalDays() != null) {
//            endDate = startDate.plusDays(original.getTotalDays() - 1);
//        }
//
//        Itinerary clone = Itinerary.builder()
//                .title(original.getTitle())
//                .description(original.getDescription())
//                .theme(original.getTheme())
//                .userId(currentUserId)
//                .status(ItineraryStatus.DRAFT)
//                .isAdminCreated(false)
//                .isPublic(false)
//                .sourceId(original.getId())
//                .startDate(startDate)
//                .endDate(endDate)
//                .totalDays(original.getTotalDays())
//                .estimatedBudget(original.getEstimatedBudget())
//                .images(new ArrayList<>())
//                .items(new ArrayList<>())
//                .build();
//
//        // ✅ CLONE IMAGES PROPERLY
//        original.getImages().forEach(img -> {
//            ItineraryImage clonedImage = ItineraryImage.builder()
//                    .url(img.getUrl())
//                    .sortOrder(img.getSortOrder())
//                    .isCover(img.getIsCover())
//                    .build();
//            clone.addImage(clonedImage);
//        });
//
//        // Clone items
//        original.getItems().forEach(item -> {
//            ItineraryItem newItem = ItineraryItem.builder()
//                    .itinerary(clone)
//                    .destination(item.getDestination())
//                    .title(item.getTitle())
//                    .notes(item.getNotes())
//                    .dayNumber(item.getDayNumber())
//                    .orderInDay(item.getOrderInDay())
//                    .startTime(item.getStartTime())
//                    .endTime(item.getEndTime())
//                    .activityType(item.getActivityType())
//                    .isVisited(false)
//                    .build();
//            clone.addItem(newItem);
//        });
//
//        original.setCopyCount(original.getCopyCount() + 1);
//        itineraryRepository.save(original);
//
//        return itineraryMapper.toResponse(itineraryRepository.save(clone), currentUserId);
//    }
//
//
//    @Override
//    public ItineraryResponse shareTrip(Long id, Long userId) {
//        Itinerary trip = getOwnedItinerary(id, userId);
//
//        if (trip.getSourceId() != null) {
//            throw new BadRequestException("Copied trips cannot be shared");
//        }
//
//        trip.setIsPublic(true);
//        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
//    }
//
//    @Override
//    public ItineraryResponse unshareTrip(Long id, Long userId) {
//        Itinerary trip = getOwnedItinerary(id, userId);
//
//        if (!Boolean.TRUE.equals(trip.getIsPublic())) {
//            throw new BadRequestException("Trip is already private");
//        }
//
//        trip.setIsPublic(false);
//        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
//    }
//
//    @Override
//    public ItineraryResponse completeTrip(Long itineraryId, Long userId) {
//        Itinerary trip = getOwnedItinerary(itineraryId, userId);
//
//        if (trip.getItems().isEmpty())
//            throw new BadRequestException("Cannot complete empty trip");
//
//        trip.setStatus(ItineraryStatus.COMPLETED);
//        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
//    }
//
//    @Override
//    public ItineraryResponse uploadItineraryImages(Long id, List<MultipartFile> files, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(id, userId);
//
//        if (files != null && !files.isEmpty()) {
//            List<String> uploadedUrls =
//                    cloudinaryStorageService.uploadMultipleFiles(files, "admin_templates");
//
//            int sortOrderStart = itinerary.getImages().size();
//
//            for (int i = 0; i < uploadedUrls.size(); i++) {
//                ItineraryImage image = ItineraryImage.builder()
//                        .url(uploadedUrls.get(i))
//                        .sortOrder(sortOrderStart + i)
//                        .isCover(false)
//                        .build();
//                itinerary.addImage(image);
//            }
//        }
//        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
//    }
//
//
//    @Override
//    @Transactional
//    public void deleteItinerary(Long id, Long userId) {
//        Itinerary itinerary = itineraryRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));
//
//        if (!itinerary.getUserId().equals(userId)) {
//            throw new AccessDeniedException("You do not have permission to delete this itinerary");
//        }
//
//        itineraryRepository.delete(itinerary);
//    }
//
//    @Override
//    @Transactional
//    public void deleteAllMyItineraries(Long userId) {
//        // Fetch EVERYTHING belonging to this user
//        List<Itinerary> userItineraries = itineraryRepository.findByUserId(userId);
//
//        if (!userItineraries.isEmpty()) {
//            itineraryRepository.deleteAll(userItineraries);
//        }
//    }
//
//    // ================= ADMIN =================
//
//    @Override
//    public ItineraryResponse createAdminTemplate(ItineraryRequest request) {
//        Itinerary template = Itinerary.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .theme(request.getTheme())
//                .status(ItineraryStatus.TEMPLATE)
//                .isAdminCreated(true)
//                .isPublic(true)
//                .totalDays(request.getTotalDays())
//                .images(new ArrayList<>())
//                .build();
//
//        if (request.getImages() != null) {
//            request.getImages().forEach(imgReq -> {
//                ItineraryImage image = ItineraryImage.builder()
//                        .url(imgReq.getUrl())
//                        .sortOrder(imgReq.getSortOrder())
//                        .isCover(Boolean.TRUE.equals(imgReq.getIsCover()))
//                        .build();
//                template.addImage(image);
//            });
//        }
//
//        return itineraryMapper.toResponse(
//                itineraryRepository.save(template),
//                getCurrentUserIdOrNull()
//        );
//    }
//
//
//    @Override
//    public ItineraryResponse createAdminTemplate(
//            ItineraryRequest request,
//            List<MultipartFile> files
//    ) {
//        Itinerary template = Itinerary.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .theme(request.getTheme())
//                .status(ItineraryStatus.TEMPLATE)
//                .isAdminCreated(true)
//                .isPublic(true)
//                .totalDays(request.getTotalDays())
//                .estimatedBudget(request.getEstimatedBudget())
//                .images(new ArrayList<>())
//                .build();
//
//        itineraryRepository.save(template);
//
//        if (files != null && !files.isEmpty()) {
//            List<String> uploadedUrls =
//                    cloudinaryStorageService.uploadMultipleFiles(files, "admin_templates");
//
//            for (int i = 0; i < uploadedUrls.size(); i++) {
//                ItineraryImage image = ItineraryImage.builder()
//                        .url(uploadedUrls.get(i))
//                        .sortOrder(i)
//                        .isCover(false)
//                        .build();
//                template.addImage(image);
//            }
//        }
//
//        return itineraryMapper.toResponse(
//                itineraryRepository.save(template),
//                getCurrentUserIdOrNull()
//        );
//    }
//
//
//
//    @Override
//    public ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));
//
//        template.setTitle(request.getTitle());
//        template.setDescription(request.getDescription());
//        template.setTheme(request.getTheme());
//        template.setTotalDays(request.getTotalDays());
//
//        if (request.getImages() != null) {
//            template.getImages().clear();
//
//            request.getImages().forEach(imgReq -> {
//                ItineraryImage image = ItineraryImage.builder()
//                        .url(imgReq.getUrl())
//                        .sortOrder(imgReq.getSortOrder())
//                        .isCover(Boolean.TRUE.equals(imgReq.getIsCover()))
//                        .build();
//                template.addImage(image);
//            });
//        }
//
//        return itineraryMapper.toResponse(
//                itineraryRepository.save(template),
//                getCurrentUserIdOrNull()
//        );
//    }
//
//
//    @Override
//    public void removeAdminTemplate(Long templateId) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));
//
//        itineraryRepository.delete(template);
//    }
//
//    @Override
//    public ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest request) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));
//
//        validateDuplicate(template, request);
//
//        var destination = destinationRepository.findById(request.getDestinationId())
//                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId()));
//
//        ItineraryItem item = ItineraryItem.builder()
//                .itinerary(template)
//                .destination(destination)
//                .title(request.getTitle())
//                .dayNumber(request.getDayNumber())
//                .orderInDay(request.getOrderInDay())
//                .activityType(request.getActivityType())
//                .build();
//
//        template.addItem(item);
//        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
//    }
//
//    @Override
//    public ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest request) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));
//
//        ItineraryItem item = template.getItems().stream()
//                .filter(i -> i.getId().equals(itemId))
//                .findFirst()
//                .orElseThrow(() -> new ResourceNotFoundException("ItineraryItem", "id", itemId));
//
//        validateDuplicate(template, request, itemId);
//
//        item.setTitle(request.getTitle());
//        item.setDayNumber(request.getDayNumber());
//        item.setOrderInDay(request.getOrderInDay());
//        item.setActivityType(request.getActivityType());
//
//        if (request.getDestinationId() != null) {
//            item.setDestination(destinationRepository.findById(request.getDestinationId())
//                    .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId())));
//        }
//
//        return itineraryMapper.toResponse(itineraryRepository.save(template), getCurrentUserIdOrNull());
//    }
//
//    @Override
//    public void removeItemFromTemplate(Long templateId, Long itemId) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new ResourceNotFoundException("Admin Template", "id", templateId));
//
//        boolean removed = template.getItems().removeIf(i -> i.getId().equals(itemId));
//        if (!removed) throw new ResourceNotFoundException("ItineraryItem", "id", itemId);
//
//        itineraryRepository.save(template);
//    }
//
//    @Override
//    public ItineraryResponse uploadAdminTemplateImages(Long templateId, List<MultipartFile> files) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("Admin Template", "id", templateId)
//                );
//
//        if (files != null && !files.isEmpty()) {
//            List<String> uploadedUrls =
//                    cloudinaryStorageService.uploadMultipleFiles(files, "admin_templates");
//
//            int startOrder = template.getImages().size();
//
//            for (int i = 0; i < uploadedUrls.size(); i++) {
//                ItineraryImage image = ItineraryImage.builder()
//                        .url(uploadedUrls.get(i))
//                        .sortOrder(startOrder + i)
//                        .isCover(false)
//                        .build();
//                template.addImage(image);
//            }
//        }
//
//        return itineraryMapper.toResponse(
//                itineraryRepository.save(template),
//                getCurrentUserIdOrNull()
//        );
//    }
//
//    @Override
//    public ItineraryResponse removeAdminTemplateImage(Long templateId, Long imageId) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("Admin Template", "id", templateId)
//                );
//
//        ItineraryImage image = template.getImages().stream()
//                .filter(img -> img.getId().equals(imageId))
//                .findFirst()
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("ItineraryImage", "id", imageId)
//                );
//
//        boolean wasCover = Boolean.TRUE.equals(image.getIsCover());
//
//        template.removeImage(image);
//
//        // If cover image removed → auto assign next one
//        if (wasCover && !template.getImages().isEmpty()) {
//            template.getImages().get(0).setIsCover(true);
//        }
//
//        return itineraryMapper.toResponse(
//                itineraryRepository.save(template),
//                getCurrentUserIdOrNull()
//        );
//    }
//
//    @Override
//    public ItineraryResponse setAdminTemplateCoverImage(Long templateId, Long imageId) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("Admin Template", "id", templateId)
//                );
//
//        ItineraryImage image = template.getImages().stream()
//                .filter(img -> img.getId().equals(imageId))
//                .findFirst()
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("ItineraryImage", "id", imageId)
//                );
//
//        template.setCoverImage(image);
//
//        return itineraryMapper.toResponse(
//                itineraryRepository.save(template),
//                getCurrentUserIdOrNull()
//        );
//    }
//
//
//
//    // ================= Save and Unsaved ================
//    @Override
//    @Transactional
//    public ItineraryResponse saveItinerary(Long itineraryId, Long userId) {
//        Itinerary itinerary = itineraryRepository.findById(itineraryId)
//                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));
//
//        if (savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId)) {
//            throw new BadRequestException("Itinerary already saved");
//        }
//
//        SavedItinerary saved = SavedItinerary.builder()
//                .itinerary(itinerary)
//                .userId(userId)
//                .savedAt(LocalDateTime.now())
//                .build();
//
//        savedItineraryRepository.save(saved);
//        return itineraryMapper.toResponse(itinerary, userId);
//    }
//
//    @Override
//    @Transactional
//    public ItineraryResponse unsaveItinerary(Long itineraryId, Long userId) {
//        // Check if it exists first
//        if (!savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId)) {
//            throw new ResourceNotFoundException("SavedItinerary", "itineraryId", itineraryId);
//        }
//
//        // Use a direct delete query
//        savedItineraryRepository.deleteByItineraryIdAndUserId(itineraryId, userId);
//
//        // CRITICAL: Force the persistence context to sync with the DB
//        savedItineraryRepository.flush();
//
//        Itinerary itinerary = itineraryRepository.findById(itineraryId)
//                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));
//
//        return itineraryMapper.toResponse(itinerary, userId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ItineraryResponse> getMySavedItineraries(Long userId) {
//        return savedItineraryRepository.findByUserId(userId).stream()
//                .map(saved -> itineraryMapper.toResponse(saved.getItinerary(), userId))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean isItinerarySaved(Long itineraryId, Long userId) {
//        return savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);
//    }
//
//
//    // ==================== Like Toggle =============================
//    @Override
//    public ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId) {
//        Itinerary itinerary = itineraryRepository.findById(itineraryId)
//                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));
//
//        // Store the itinerary in a final variable for lambda
//        final Itinerary finalItinerary = itinerary;
//
//        userLikedItineraryRepository.findByItineraryIdAndUserId(itineraryId, userId)
//                .ifPresentOrElse(
//                        like -> {
//                            userLikedItineraryRepository.delete(like);
//                            // Use finalItinerary instead of itinerary
//                            finalItinerary.setLikeCount(Math.max(0, finalItinerary.getLikeCount() - 1));
//                        },
//                        () -> {
//                            userLikedItineraryRepository.save(UserLikedItinerary.builder()
//                                    .itinerary(finalItinerary)
//                                    .userId(userId)
//                                    .likedAt(LocalDateTime.now())
//                                    .build());
//                            // Use finalItinerary instead of itinerary
//                            finalItinerary.setLikeCount(finalItinerary.getLikeCount() + 1);
//                        }
//                );
//
//        // Save the itinerary
//        itinerary = itineraryRepository.save(finalItinerary);
//
//        // Get the ACTUAL like count from database - convert Long to Integer
//        Long actualLikeCountLong = userLikedItineraryRepository.countByItineraryId(itineraryId);
//        Integer actualLikeCount = actualLikeCountLong != null ? actualLikeCountLong.intValue() : 0;
//        itinerary.setLikeCount(actualLikeCount);
//
//        return itineraryMapper.toResponse(itinerary, userId);
//    }
//
//    // ================= PRIVATE HELPERS =================
//
//    private Integer calculateDuration(LocalDate start, LocalDate end) {
//        if (start == null || end == null) {
//            return null; // Allows for "Dream Trips"
//        }
//        if (end.isBefore(start)) {
//            throw new BadRequestException("End date cannot be before start date.");
//        }
//        // +1 because a trip from Monday to Monday is 1 day, but usually users mean 1 day of activities
//        // ChronoUnit.DAYS.between(start, end) is standard, but check if your UI expects (days + 1)
//        return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
//    }
//
//    private Itinerary getOwnedItinerary(Long itineraryId, Long userId) {
//        return itineraryRepository.findById(itineraryId)
//                .filter(it -> Objects.equals(it.getUserId(), userId))
//                .orElseThrow(() -> new ForbiddenException("You do not have permission to access this itinerary"));
//    }
//
//    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request) {
//        validateDuplicate(itinerary, request, null);
//    }
//
//    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request, Long ignoreItemId) {
//        boolean exists = itinerary.getItems().stream()
//                .anyMatch(i -> !Objects.equals(i.getId(), ignoreItemId)
//                        && i.getDestination().getId().equals(request.getDestinationId())
//                        && i.getDayNumber().equals(request.getDayNumber()));
//        if (exists) throw new BadRequestException("This destination is already planned for this day.");
//    }
//
//    private void validateDuplicateInRequest(List<ItineraryItemRequest> items) {
//        long uniqueCount = items.stream()
//                .map(i -> i.getDestinationId() + "-" + i.getDayNumber())
//                .distinct().count();
//        if (uniqueCount != items.size()) throw new BadRequestException("Duplicate destinations found in your plan.");
//    }
//
//    private Specification<Itinerary> isPublicAndCompleted() {
//        return (root, query, cb) -> cb.and(
//                cb.equal(root.get("isPublic"), true),
//                cb.equal(root.get("status"), ItineraryStatus.COMPLETED),
//                cb.equal(root.get("isAdminCreated"), false),
//                cb.isNull(root.get("sourceId"))
//        );
//    }
//
//    private Specification<Itinerary> hasTheme(String theme) {
//        return (theme == null || theme.isBlank()) ? null : (root, query, cb) -> cb.equal(root.get("theme"), theme);
//    }
//
//    private Specification<Itinerary> matchesSearch(String search) {
//        return (search == null || search.isBlank()) ? null : (root, query, cb) ->
//                cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
//    }
//}