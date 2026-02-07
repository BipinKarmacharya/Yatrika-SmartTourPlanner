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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return itineraryRepository.findByStatusAndIsPublicTrueAndIsAdminCreatedFalseAndSourceIdIsNull(
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
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));

        // Only allow if: public OR owned by currentUser
        if (!Boolean.TRUE.equals(itinerary.getIsPublic()) && !currentUserId.equals(itinerary.getUserId())) {
            throw new RuntimeException("Access Denied: Not allowed to view this itinerary");
        }

        return itineraryMapper.toResponse(itinerary, currentUserId);
    }

    // ================= PERSONAL MANAGEMENT =================

    @Override
    public ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId) {
        Itinerary trip = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .userId(userId)
                .status(ItineraryStatus.DRAFT)
                .isAdminCreated(false)
                .isPublic(false)
                .build();
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    public Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable) {
        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(itinerary -> itineraryMapper.toResponse(itinerary, userId));
    }

    @Override
    public ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(id, userId);
        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setTheme(request.getTheme());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setEstimatedBudget(request.getEstimatedBudget());
        return itineraryMapper.toResponse(itineraryRepository.save(itinerary), userId);
    }

    @Override
    public ItineraryResponse updateFullItinerary(Long id, ItineraryRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(id, userId);
        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());

        if (request.getItems() != null) validateDuplicateInRequest(request.getItems());

        itinerary.getItems().clear();
        itineraryRepository.saveAndFlush(itinerary);

        if (request.getItems() != null) {
            for (ItineraryItemRequest itemReq : request.getItems()) {
                if (itemReq.getDestinationId() == null) continue;
                var destination = destinationRepository.findById(itemReq.getDestinationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
                ItineraryItem newItem = ItineraryItem.builder()
                        .itinerary(itinerary)
                        .destination(destination)
                        .title(itemReq.getTitle())
                        .notes(itemReq.getNotes())
                        .dayNumber(itemReq.getDayNumber())
                        .orderInDay(itemReq.getOrderInDay())
                        .startTime(itemReq.getStartTime())
                        .endTime(itemReq.getEndTime())
                        .activityType(itemReq.getActivityType() != null ? itemReq.getActivityType() : "VISIT")
                        .isVisited(Boolean.TRUE.equals(itemReq.getIsVisited()))
                        .build();
                itinerary.addItem(newItem);
            }
        }
        return itineraryMapper.toResponse(itineraryRepository.saveAndFlush(itinerary), userId);
    }

    // ================= ITEM MANAGEMENT =================

    @Override
    public ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        validateDuplicate(itinerary, request);

        var destination = destinationRepository.getReferenceById(request.getDestinationId());
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
                .orElseThrow(() -> new EntityNotFoundException("Item not found"));

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
                    .orElseThrow(() -> new ResourceNotFoundException("Destination not found")));
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
    public ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId) {
        Itinerary original = itineraryRepository.findById(targetItineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Original itinerary not found"));

        if (original.getSourceId() != null)
            throw new IllegalStateException("Cannot copy a copied itinerary");

        Itinerary clone = Itinerary.builder()
                .title(original.getTitle() + " (Copy)")
                .description(original.getDescription())
                .theme(original.getTheme())
                .userId(currentUserId)
                .status(ItineraryStatus.DRAFT)
                .isAdminCreated(false)
                .isPublic(false)
                .sourceId(original.getId())
                .totalDays(original.getTotalDays())
                .estimatedBudget(original.getEstimatedBudget())
                .items(new ArrayList<>())
                .build();

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
                    .isVisited(false) // reset
                    .build();
            clone.addItem(newItem);
        });

        original.setCopyCount(original.getCopyCount() + 1);
        itineraryRepository.save(original);

        return itineraryMapper.toResponse(itineraryRepository.save(clone), currentUserId);
    }

    @Override
    public ItineraryResponse shareTrip(Long id, Long userId) {
        Itinerary trip = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // Prevent sharing copied trips
        if (trip.getSourceId() != null) {
            throw new BadRequestException("Copied trips cannot be shared");
        }

        // Only allow owner to share
        if (!trip.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this trip");
        }

        trip.setIsPublic(true);
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
    }

    @Override
    public ItineraryResponse unshareTrip(Long id, Long userId) {
        Itinerary trip = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // Only allow owner to unshare
        if (!trip.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this trip");
        }

        // Check if trip is already private
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
            throw new IllegalStateException("Cannot complete empty trip");

        trip.setStatus(ItineraryStatus.COMPLETED);
        return itineraryMapper.toResponse(itineraryRepository.save(trip), userId);
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
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .build();
        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryMapper.toResponse(itineraryRepository.save(template), currentUserId);
    }

    @Override
    public ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setTheme(request.getTheme());
        template.setTotalDays(request.getTotalDays());

        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryMapper.toResponse(itineraryRepository.save(template), currentUserId);
    }

    @Override
    public void removeAdminTemplate(Long templateId) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        itineraryRepository.delete(template);
    }

    @Override
    public ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        validateDuplicate(template, request);

        var destination = destinationRepository.getReferenceById(request.getDestinationId());
        ItineraryItem item = ItineraryItem.builder()
                .itinerary(template)
                .destination(destination)
                .title(request.getTitle())
                .dayNumber(request.getDayNumber())
                .orderInDay(request.getOrderInDay())
                .activityType(request.getActivityType())
                .build();

        template.addItem(item);
        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryMapper.toResponse(itineraryRepository.save(template), currentUserId);
    }

    @Override
    public ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        ItineraryItem item = template.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Template item not found"));

        validateDuplicate(template, request, itemId);

        item.setTitle(request.getTitle());
        item.setNotes(request.getNotes());
        item.setDayNumber(request.getDayNumber());
        item.setOrderInDay(request.getOrderInDay());
        item.setActivityType(request.getActivityType());

        if (request.getDestinationId() != null) {
            item.setDestination(destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination not found")));
        }

        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryMapper.toResponse(itineraryRepository.save(template), currentUserId);
    }

    @Override
    public void removeItemFromTemplate(Long templateId, Long itemId) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        boolean removed = template.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) throw new EntityNotFoundException("Template item not found");

        itineraryRepository.save(template);
    }

    @Override
    public ItineraryResponse updateFullAdminTemplate(Long templateId, ItineraryRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setTheme(request.getTheme());
        template.getItems().clear();

        if (request.getItems() != null) {
            for (ItineraryItemRequest itemReq : request.getItems()) {
                var destination = destinationRepository.findById(itemReq.getDestinationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
                template.addItem(ItineraryItem.builder()
                        .itinerary(template)
                        .destination(destination)
                        .title(itemReq.getTitle())
                        .notes(itemReq.getNotes())
                        .dayNumber(itemReq.getDayNumber())
                        .orderInDay(itemReq.getOrderInDay())
                        .activityType(itemReq.getActivityType())
                        .imageUrl(itemReq.getImageUrl())
                        .build());
            }
        }

        Long currentUserId = getCurrentUserIdOrNull();
        return itineraryMapper.toResponse(itineraryRepository.save(template), currentUserId);
    }

    // ================= Save and Unsaved ================
    @Override
    @Transactional
    public ItineraryResponse saveItinerary(Long itineraryId, Long userId) {
        // 1. Find the itinerary
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        // 2. Check if already saved
        boolean alreadySaved = savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);

        if (alreadySaved) {
            throw new IllegalArgumentException("Itinerary already saved");
        }

        // 3. Create saved entry
        SavedItinerary saved = SavedItinerary.builder()
                .itinerary(itinerary)
                .userId(userId)
                .savedAt(LocalDateTime.now())
                .build();

        savedItineraryRepository.save(saved);

        // 4. Return updated itinerary
        return itineraryMapper.toResponse(itinerary, userId);
    }

    @Override
    @Transactional
    public ItineraryResponse unsaveItinerary(Long itineraryId, Long userId) {
        // Find and delete the saved entry
        SavedItinerary saved = savedItineraryRepository
                .findByItineraryIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Saved itinerary not found"));

        savedItineraryRepository.delete(saved);

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        return itineraryMapper.toResponse(itinerary, userId);
    }

    @Override
    public List<ItineraryResponse> getMySavedItineraries(Long userId) {
        List<SavedItinerary> savedItems = savedItineraryRepository.findByUserId(userId);
        return savedItems.stream()
                .map(saved -> {
                    Itinerary itinerary = saved.getItinerary();
                    // Make sure to fetch all related data
                    if (itinerary.getItems() != null) {
                        itinerary.getItems().size(); // Trigger lazy loading
                    }
                    return itineraryMapper.toResponse(itinerary, userId);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isItinerarySaved(Long itineraryId, Long userId) {
        return savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);
    }

    // ==================== Like Toggle =============================
    @Override
    @Transactional
    public ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        // Check if already liked
        boolean alreadyLiked = userLikedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);

        if (alreadyLiked) {
            // Unlike: find and delete the like entry
            UserLikedItinerary like = userLikedItineraryRepository
                    .findByItineraryIdAndUserId(itineraryId, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Like not found"));

            userLikedItineraryRepository.delete(like);

            // Decrement like count
            itinerary.setLikeCount(Math.max(0, itinerary.getLikeCount() - 1));
            log.info("User {} unliked itinerary {}", userId, itineraryId);
        } else {
            // Like: create like entry
            UserLikedItinerary like = UserLikedItinerary.builder()
                    .itinerary(itinerary)
                    .userId(userId)
                    .likedAt(LocalDateTime.now())
                    .build();

            userLikedItineraryRepository.save(like);

            // Increment like count
            itinerary.setLikeCount(itinerary.getLikeCount() + 1);
            log.info("User {} liked itinerary {}", userId, itineraryId);
        }

        itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(itinerary, userId);
    }

    // ================= PRIVATE HELPERS =================

    private Itinerary getOwnedItinerary(Long itineraryId, Long userId) {
        return itineraryRepository.findById(itineraryId)
                .filter(it -> Objects.equals(it.getUserId(), userId))
                .orElseThrow(() -> new RuntimeException("Itinerary not found or not owned by user"));
    }

    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request) {
        validateDuplicate(itinerary, request, null);
    }

    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request, Long ignoreItemId) {
        boolean exists = itinerary.getItems().stream()
                .anyMatch(i -> !i.getId().equals(ignoreItemId)
                        && i.getDestination().getId().equals(request.getDestinationId())
                        && i.getDayNumber().equals(request.getDayNumber()));
        if (exists) throw new IllegalStateException("Duplicate item in same day");
    }

    private void validateDuplicateInRequest(List<ItineraryItemRequest> items) {
        long uniqueCount = items.stream()
                .map(i -> i.getDestinationId() + "-" + i.getDayNumber())
                .distinct().count();
        if (uniqueCount != items.size()) throw new IllegalStateException("Duplicate items in request payload");
    }

    // Specifications for filtering public trips
    private Specification<Itinerary> isPublicAndCompleted() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isPublic"), true),
                cb.equal(root.get("status"), ItineraryStatus.COMPLETED),
                cb.equal(root.get("isAdminCreated"), false),
                cb.isNull(root.get("sourceId"))
        );
    }

    private Specification<Itinerary> hasTheme(String theme) {
        return (theme == null) ? null : (root, query, cb) -> cb.equal(root.get("theme"), theme);
    }

    private Specification<Itinerary> matchesSearch(String search) {
        return (search == null || search.isEmpty()) ? null : (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
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
//import jakarta.persistence.EntityNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
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
//
//    // ================= DISCOVERY =================
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ItineraryResponse> getAdminTemplates() {
//        return itineraryRepository.findByStatusAndIsAdminCreatedTrue(ItineraryStatus.TEMPLATE)
//                .stream().map(itineraryMapper::toResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable) {
//        return itineraryRepository.findByStatusAndIsPublicTrueAndIsAdminCreatedFalseAndSourceIdIsNull(ItineraryStatus.COMPLETED, pageable)
//                .map(itineraryMapper::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable) {
//        Specification<Itinerary> spec = Specification.where(isPublicAndCompleted())
//                .and(hasTheme(filter.getTheme()))
//                .and(matchesSearch(filter.getSearchQuery()));
//        return itineraryRepository.findAll(spec, pageable).map(itineraryMapper::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ItineraryResponse getItineraryById(Long id, Long currentUserId) {
//        Itinerary itinerary = itineraryRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));
//
//        // Only allow if: public OR owned by currentUser
//        if (!Boolean.TRUE.equals(itinerary.getIsPublic()) && !currentUserId.equals(itinerary.getUserId())) {
//            throw new RuntimeException("Access Denied: Not allowed to view this itinerary");
//        }
//
//        return itineraryMapper.toResponse(itinerary);
//    }
//
//    // ================= PERSONAL MANAGEMENT =================
//
//    @Override
//    public ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId) {
//        Itinerary trip = Itinerary.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .theme(request.getTheme())
//                .userId(userId)
//                .status(ItineraryStatus.DRAFT)
//                .isAdminCreated(false)
//                .isPublic(false)
//                .build();
//        return itineraryMapper.toResponse(itineraryRepository.save(trip));
//    }
//
//    @Override
//    public Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable) {
//        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
//                .map(itineraryMapper::toResponse);
//    }
//
//    @Override
//    public ItineraryResponse updateItineraryHeader(Long id, ItineraryRequest request, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(id, userId);
//        itinerary.setTitle(request.getTitle());
//        itinerary.setDescription(request.getDescription());
//        itinerary.setTheme(request.getTheme());
//        itinerary.setStartDate(request.getStartDate());
//        itinerary.setEndDate(request.getEndDate());
//        itinerary.setEstimatedBudget(request.getEstimatedBudget());
//        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
//    }
//
//    @Override
//    public ItineraryResponse updateFullItinerary(Long id, ItineraryRequest request, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(id, userId);
//        itinerary.setTitle(request.getTitle());
//        itinerary.setDescription(request.getDescription());
//
//        if (request.getItems() != null) validateDuplicateInRequest(request.getItems());
//
//        itinerary.getItems().clear();
//        itineraryRepository.saveAndFlush(itinerary);
//
//        if (request.getItems() != null) {
//            for (ItineraryItemRequest itemReq : request.getItems()) {
//                if (itemReq.getDestinationId() == null) continue;
//                var destination = destinationRepository.findById(itemReq.getDestinationId())
//                        .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
//                ItineraryItem newItem = ItineraryItem.builder()
//                        .itinerary(itinerary)
//                        .destination(destination)
//                        .title(itemReq.getTitle())
//                        .notes(itemReq.getNotes())
//                        .dayNumber(itemReq.getDayNumber())
//                        .orderInDay(itemReq.getOrderInDay())
//                        .startTime(itemReq.getStartTime())
//                        .endTime(itemReq.getEndTime())
//                        .activityType(itemReq.getActivityType() != null ? itemReq.getActivityType() : "VISIT")
//                        .isVisited(Boolean.TRUE.equals(itemReq.getIsVisited()))
//                        .build();
//                itinerary.addItem(newItem);
//            }
//        }
//        return itineraryMapper.toResponse(itineraryRepository.saveAndFlush(itinerary));
//    }
//
//    // ================= ITEM MANAGEMENT =================
//
//    @Override
//    public ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest request, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
//        validateDuplicate(itinerary, request);
//
//        var destination = destinationRepository.getReferenceById(request.getDestinationId());
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
//        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
//    }
//
//    @Override
//    public ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId) {
//        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
//
//        ItineraryItem item = itinerary.getItems().stream()
//                .filter(i -> i.getId().equals(itemId))
//                .findFirst()
//                .orElseThrow(() -> new EntityNotFoundException("Item not found"));
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
//                    .orElseThrow(() -> new ResourceNotFoundException("Destination not found")));
//        }
//
//        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
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
//    public ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId) {
//        Itinerary original = itineraryRepository.findById(targetItineraryId)
//                .orElseThrow(() -> new ResourceNotFoundException("Original itinerary not found"));
//
//        if (original.getSourceId() != null)
//            throw new IllegalStateException("Cannot copy a copied itinerary");
//
//        Itinerary clone = Itinerary.builder()
//                .title(original.getTitle() + " (Copy)")
//                .description(original.getDescription())
//                .theme(original.getTheme())
//                .userId(currentUserId)
//                .status(ItineraryStatus.DRAFT)
//                .isAdminCreated(false)
//                .isPublic(false)
//                .sourceId(original.getId())
//                .totalDays(original.getTotalDays())
//                .estimatedBudget(original.getEstimatedBudget())
//                .items(new ArrayList<>())
//                .build();
//
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
//                    .isVisited(false) // reset
//                    .build();
//            clone.addItem(newItem);
//        });
//
//        original.setCopyCount(original.getCopyCount() + 1);
//        itineraryRepository.save(original);
//
//        return itineraryMapper.toResponse(itineraryRepository.save(clone));
//    }
//
//    @Override
//    public ItineraryResponse shareTrip(Long id, Long userId) {
//        Itinerary trip = itineraryRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
//
//        // Prevent sharing copied trips
//        if (trip.getSourceId() != null) {
//            throw new BadRequestException("Copied trips cannot be shared");
//        }
//
//        // Only allow owner to share
//        if (!trip.getUserId().equals(userId)) {
//            throw new ForbiddenException("You do not own this trip");
//        }
//
//        trip.setIsPublic(true);
//        return itineraryMapper.toResponse(itineraryRepository.save(trip));
//    }
//
//    @Override
//    public ItineraryResponse unshareTrip(Long id, Long userId) {
//        Itinerary trip = itineraryRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
//
//        // Only allow owner to unshare
//        if (!trip.getUserId().equals(userId)) {
//            throw new ForbiddenException("You do not own this trip");
//        }
//
//        // Check if trip is already private
//        if (!Boolean.TRUE.equals(trip.getIsPublic())) {
//            throw new BadRequestException("Trip is already private");
//        }
//
//        trip.setIsPublic(false);
//        return itineraryMapper.toResponse(itineraryRepository.save(trip));
//    }
//
//    @Override
//    public ItineraryResponse completeTrip(Long itineraryId, Long userId) {
//        Itinerary trip = getOwnedItinerary(itineraryId, userId);
//
//        if (trip.getItems().isEmpty())
//            throw new IllegalStateException("Cannot complete empty trip");
//
//        trip.setStatus(ItineraryStatus.COMPLETED);
//        return itineraryMapper.toResponse(itineraryRepository.save(trip));
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
//                .build();
//        return itineraryMapper.toResponse(itineraryRepository.save(template));
//    }
//
//    @Override
//    public ItineraryResponse updateAdminTemplate(Long templateId, ItineraryRequest request) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
//
//        template.setTitle(request.getTitle());
//        template.setDescription(request.getDescription());
//        template.setTheme(request.getTheme());
//        template.setTotalDays(request.getTotalDays());
//
//        return itineraryMapper.toResponse(itineraryRepository.save(template));
//    }
//
//    @Override
//    public void removeAdminTemplate(Long templateId) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
//
//        itineraryRepository.delete(template);
//    }
//
//    @Override
//    public ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest request) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
//
//        validateDuplicate(template, request);
//
//        var destination = destinationRepository.getReferenceById(request.getDestinationId());
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
//        return itineraryMapper.toResponse(itineraryRepository.save(template));
//    }
//
//    @Override
//    public ItineraryResponse updateTemplateItem(Long templateId, Long itemId, ItineraryItemRequest request) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
//
//        ItineraryItem item = template.getItems().stream()
//                .filter(i -> i.getId().equals(itemId))
//                .findFirst()
//                .orElseThrow(() -> new EntityNotFoundException("Template item not found"));
//
//        validateDuplicate(template, request, itemId);
//
//        item.setTitle(request.getTitle());
//        item.setNotes(request.getNotes());
//        item.setDayNumber(request.getDayNumber());
//        item.setOrderInDay(request.getOrderInDay());
//        item.setActivityType(request.getActivityType());
//
//        if (request.getDestinationId() != null) {
//            item.setDestination(destinationRepository.findById(request.getDestinationId())
//                    .orElseThrow(() -> new ResourceNotFoundException("Destination not found")));
//        }
//
//        return itineraryMapper.toResponse(itineraryRepository.save(template));
//    }
//
//    @Override
//    public void removeItemFromTemplate(Long templateId, Long itemId) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
//
//        boolean removed = template.getItems().removeIf(i -> i.getId().equals(itemId));
//        if (!removed) throw new EntityNotFoundException("Template item not found");
//
//        itineraryRepository.save(template);
//    }
//
//    @Override
//    public ItineraryResponse updateFullAdminTemplate(Long templateId, ItineraryRequest request) {
//        Itinerary template = itineraryRepository.findById(templateId)
//                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE && it.getIsAdminCreated())
//                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
//
//        template.setTitle(request.getTitle());
//        template.setDescription(request.getDescription());
//        template.setTheme(request.getTheme());
//        template.getItems().clear();
//
//        if (request.getItems() != null) {
//            for (ItineraryItemRequest itemReq : request.getItems()) {
//                var destination = destinationRepository.findById(itemReq.getDestinationId())
//                        .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
//                template.addItem(ItineraryItem.builder()
//                        .itinerary(template)
//                        .destination(destination)
//                        .title(itemReq.getTitle())
//                        .notes(itemReq.getNotes())
//                        .dayNumber(itemReq.getDayNumber())
//                        .orderInDay(itemReq.getOrderInDay())
//                        .activityType(itemReq.getActivityType())
//                        .build());
//            }
//        }
//
//        return itineraryMapper.toResponse(itineraryRepository.save(template));
//    }
//
//
//    // ================= Save and Unsaved ================
//    @Override
//    @Transactional
//    public ItineraryResponse saveItinerary(Long itineraryId, Long userId) {
//        // 1. Find the itinerary
//        Itinerary itinerary = itineraryRepository.findById(itineraryId)
//                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));
//
//        // 2. Check if already saved
//        boolean alreadySaved = savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);
//
//        if (alreadySaved) {
//            throw new IllegalArgumentException("Itinerary already saved");
//        }
//
//        // 3. Create saved entry
//        SavedItinerary saved = SavedItinerary.builder()
//                .itinerary(itinerary)
//                .userId(userId)
//                .savedAt(LocalDateTime.now())
//                .build();
//
//        savedItineraryRepository.save(saved);
//
//        // 4. Return updated itinerary (optional: increment save count)
//        return itineraryMapper.toResponse(itinerary);
//    }
//
//    @Override
//    @Transactional
//    public ItineraryResponse unsaveItinerary(Long itineraryId, Long userId) {
//        // Find and delete the saved entry
//        SavedItinerary saved = savedItineraryRepository
//                .findByItineraryIdAndUserId(itineraryId, userId)
//                .orElseThrow(() -> new EntityNotFoundException("Saved itinerary not found"));
//
//        savedItineraryRepository.delete(saved);
//
//        Itinerary itinerary = itineraryRepository.findById(itineraryId)
//                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));
//
//        return itineraryMapper.toResponse(itinerary);
//    }
//
//    @Override
//    public List<ItineraryResponse> getMySavedItineraries(Long userId) {
//        List<SavedItinerary> savedItems = savedItineraryRepository.findByUserId(userId);
//        return savedItems.stream()
//                .map(saved -> {
//                    Itinerary itinerary = saved.getItinerary();
//                    // Make sure to fetch all related data
//                    if (itinerary.getItems() != null) {
//                        itinerary.getItems().size(); // Trigger lazy loading
//                    }
//                    return itineraryMapper.toResponse(itinerary);
//                })
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public boolean isItinerarySaved(Long itineraryId, Long userId) {
//        return savedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);
//    }
//
//    // ==================== Like nad Unlike =============================
//    // ==================== Like Toggle =============================
//    @Override
//    @Transactional
//    public ItineraryResponse toggleLikeItinerary(Long itineraryId, Long userId) {
//        Itinerary itinerary = itineraryRepository.findById(itineraryId)
//                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));
//
//        // Check if already liked
//        boolean alreadyLiked = userLikedItineraryRepository.existsByItineraryIdAndUserId(itineraryId, userId);
//
//        if (alreadyLiked) {
//            // Unlike: find and delete the like entry
//            UserLikedItinerary like = userLikedItineraryRepository
//                    .findByItineraryIdAndUserId(itineraryId, userId)
//                    .orElseThrow(() -> new EntityNotFoundException("Like not found"));
//
//            userLikedItineraryRepository.delete(like);
//
//            // Decrement like count
//            itinerary.setLikeCount(Math.max(0, itinerary.getLikeCount() - 1));
//            log.info("User {} unliked itinerary {}", userId, itineraryId);
//        } else {
//            // Like: create like entry
//            UserLikedItinerary like = UserLikedItinerary.builder()
//                    .itinerary(itinerary)
//                    .userId(userId)
//                    .likedAt(LocalDateTime.now())
//                    .build();
//
//            userLikedItineraryRepository.save(like);
//
//            // Increment like count
//            itinerary.setLikeCount(itinerary.getLikeCount() + 1);
//            log.info("User {} liked itinerary {}", userId, itineraryId);
//        }
//
//        itineraryRepository.save(itinerary);
//        return itineraryMapper.toResponse(itinerary, userId);
//    }
//
//    // ================= PRIVATE HELPERS =================
//
//    private Itinerary getOwnedItinerary(Long itineraryId, Long userId) {
//        return itineraryRepository.findById(itineraryId)
//                .filter(it -> Objects.equals(it.getUserId(), userId))
//                .orElseThrow(() -> new RuntimeException("Itinerary not found or not owned by user"));
//    }
//
//    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request) {
//        validateDuplicate(itinerary, request, null);
//    }
//
//    private void validateDuplicate(Itinerary itinerary, ItineraryItemRequest request, Long ignoreItemId) {
//        boolean exists = itinerary.getItems().stream()
//                .anyMatch(i -> !i.getId().equals(ignoreItemId)
//                        && i.getDestination().getId().equals(request.getDestinationId())
//                        && i.getDayNumber().equals(request.getDayNumber()));
//        if (exists) throw new IllegalStateException("Duplicate item in same day");
//    }
//
//    private void validateDuplicateInRequest(List<ItineraryItemRequest> items) {
//        long uniqueCount = items.stream()
//                .map(i -> i.getDestinationId() + "-" + i.getDayNumber())
//                .distinct().count();
//        if (uniqueCount != items.size()) throw new IllegalStateException("Duplicate items in request payload");
//    }
//
//    // Specifications for filtering public trips
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
//        return (theme == null) ? null : (root, query, cb) -> cb.equal(root.get("theme"), theme);
//    }
//
//    private Specification<Itinerary> matchesSearch(String search) {
//        return (search == null || search.isEmpty()) ? null : (root, query, cb) ->
//                cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
//    }
//}
