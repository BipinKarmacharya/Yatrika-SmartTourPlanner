package com.yatrika.itinerary.service.impl;

import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.domain.ItineraryStatus;
import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest; // Ensure this exists
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.dto.response.ItinerarySummary;
import com.yatrika.itinerary.mapper.ItineraryMapper;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.itinerary.service.ItineraryService;
import com.yatrika.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryMapper itineraryMapper;
    private final DestinationRepository destinationRepository;

    // --- IMPLEMENTING MISSING METHODS TO FIX COMPILER ERRORS ---

    @Override
    public ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId) {
        Itinerary newTrip = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(userId)
                .theme(request.getTheme())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .estimatedBudget(request.getEstimatedBudget())
                .status(ItineraryStatus.DRAFT)
                .isPublic(false)
                .isAdminCreated(false)
                .build();

        return itineraryMapper.toResponse(itineraryRepository.save(newTrip));
    }

    // New method to allow users to add specific destinations to their plan
    @Override
    @Transactional
    public ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest itemRequest, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        // Security check: Only owner can add items
        if (!itinerary.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This is not your trip.");
        }

        // Convert Request to Entity (Manually or via Mapper)
        ItineraryItem newItem = ItineraryItem.builder()
                .itinerary(itinerary)
                .destination(destinationRepository.getReferenceById(itemRequest.getDestinationId()))
                .dayNumber(itemRequest.getDayNumber())
                .orderInDay(itemRequest.getOrderInDay())
                .title(itemRequest.getTitle())
                .notes(itemRequest.getNotes())
                .startTime(itemRequest.getStartTime())
                .endTime(itemRequest.getEndTime())
                .activityType(itemRequest.getActivityType())
                .build();

        itinerary.addItem(newItem);
        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
    }

    @Override
    public List<ItineraryResponse> getAdminTemplates() {
        return itineraryRepository.findByStatusAndIsAdminCreatedTrue(ItineraryStatus.TEMPLATE)
                .stream()
                .map(itineraryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ItineraryResponse getItineraryById(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + id));
        return itineraryMapper.toResponse(itinerary);
    }

    @Override
    public Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable) {
        return itineraryRepository.findByStatusAndIsPublicTrueAndIsAdminCreatedFalse(
                        ItineraryStatus.COMPLETED, pageable)
                .map(itineraryMapper::toResponse);
    }

    // --- THE UPDATED COPY LOGIC ---

    @Override
    public ItineraryResponse copyItinerary(Long targetItineraryId, Long currentUserId) {
        Itinerary source = itineraryRepository.findById(targetItineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        validateCopyEligibility(source);

        Itinerary clone = Itinerary.builder()
                .title("Copy of " + source.getTitle())
                .description(source.getDescription())
                .userId(currentUserId)
                .status(ItineraryStatus.DRAFT)
                .isPublic(false)
                .isAdminCreated(false)
                .sourceId(source.getId())
                .theme(source.getTheme())
                .totalDays(source.getTotalDays())
                .estimatedBudget(source.getEstimatedBudget())
                .build();

        if (source.getItems() != null) {
            for (ItineraryItem sourceItem : source.getItems()) {
                clone.addItem(cloneItem(sourceItem, clone));
            }
        }

        Itinerary savedClone = itineraryRepository.save(clone);

        // Update analytics on original
        source.setCopyCount(source.getCopyCount() + 1);

        return itineraryMapper.toResponse(savedClone);
    }

    // --- RE-SHARE PROTECTION ---

    @Override
    public ItineraryResponse shareTrip(Long itineraryId, Long currentUserId) {
        Itinerary trip = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found"));

        // Fixed: Use a RuntimeException or Spring's AccessDeniedException
        if (!trip.getUserId().equals(currentUserId)) {
            throw new RuntimeException("Access Denied: You don't own this trip.");
        }

        if (trip.getSourceId() != null) {
            throw new IllegalStateException("Copied trips cannot be shared to the public tab.");
        }

        if (trip.getStatus() != ItineraryStatus.COMPLETED) {
            throw new IllegalStateException("Only completed trips can be shared.");
        }

        trip.setIsPublic(true);
        return itineraryMapper.toResponse(itineraryRepository.save(trip));
    }

    // --- PRIVATE HELPERS ---

    private void validateCopyEligibility(Itinerary source) {
        boolean isTemplate = source.getStatus() == ItineraryStatus.TEMPLATE;
        boolean isPublicCompleted = Boolean.TRUE.equals(source.getIsPublic())
                && source.getStatus() == ItineraryStatus.COMPLETED;

        if (!isTemplate && !isPublicCompleted) {
            throw new IllegalStateException("Only Admin templates or public completed trips can be copied.");
        }
    }

    private ItineraryItem cloneItem(ItineraryItem sourceItem, Itinerary parent) {
        return ItineraryItem.builder()
                .itinerary(parent)
                .destination(sourceItem.getDestination())
                .dayNumber(sourceItem.getDayNumber())
                .orderInDay(sourceItem.getOrderInDay())
                .title(sourceItem.getTitle())
                .notes(sourceItem.getNotes())
                .startTime(sourceItem.getStartTime())
                .endTime(sourceItem.getEndTime())
                .activityType(sourceItem.getActivityType())
                .build();
    }


    @Override
    @Transactional
    public ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        // Security check
        if (!itinerary.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You don't own this trip.");
        }

        // Find the specific item within that itinerary
        ItineraryItem item = itinerary.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Item not found in this itinerary"));

        // Update fields
        item.setTitle(request.getTitle());
        item.setNotes(request.getNotes());
        item.setDayNumber(request.getDayNumber());
        item.setOrderInDay(request.getOrderInDay());
        item.setStartTime(request.getStartTime());
        item.setEndTime(request.getEndTime());
        item.setActivityType(request.getActivityType());

        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
    }

    @Override
    @Transactional
    public void removeItem(Long itineraryId, Long itemId, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));

        if (!itinerary.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // This uses the 'orphanRemoval = true' logic from your Entity
        itinerary.getItems().removeIf(item -> item.getId().equals(itemId));

        itineraryRepository.save(itinerary);
    }

    @Override
    public Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable) {
        // We reuse the query we defined in the Repository earlier
        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(itineraryMapper::toResponse);
    }


    // ----- Admin ------
    @Override
    @Transactional
    public ItineraryResponse createAdminTemplate(ItineraryRequest request) {
        Itinerary template = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(null) // Admins don't "own" templates personally
                .status(ItineraryStatus.TEMPLATE)
                .isAdminCreated(true)
                .isPublic(true) // Templates are always visible in Tab 2
                .theme(request.getTheme())
                .totalDays(request.getTotalDays())
                .build();

        return itineraryMapper.toResponse(itineraryRepository.save(template));
    }

    @Override
    @Transactional
    public ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest itemRequest) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        ItineraryItem newItem = ItineraryItem.builder()
                .itinerary(template)
                .destination(destinationRepository.getReferenceById(itemRequest.getDestinationId()))
                .dayNumber(itemRequest.getDayNumber())
                .orderInDay(itemRequest.getOrderInDay())
                .title(itemRequest.getTitle())
                .activityType(itemRequest.getActivityType())
                .build();

        template.addItem(newItem);
        return itineraryMapper.toResponse(itineraryRepository.save(template));
    }

    // ------ Filter ------
    @Override
    public Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable) {
        Specification<Itinerary> spec = Specification.where(isPublicAndCompleted())
                .and(hasTheme(filter.getTheme()))
                .and(hasBudget(filter.getBudgetRange()))
                .and(matchesSearch(filter.getSearchQuery()));

        return itineraryRepository.findAll(spec, pageable)
                .map(itineraryMapper::toResponse);
    }

    // ---- DRAFT to COMPLETE ---
    @Override
    @Transactional
    public ItineraryResponse completeTrip(Long itineraryId, Long userId) {
        Itinerary trip = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found"));

        // Ownership check
        if (!trip.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Validation: Cannot complete an empty trip
        if (trip.getItems() == null || trip.getItems().isEmpty()) {
            throw new IllegalStateException("You cannot complete an itinerary with no activities.");
        }

        // State change
        trip.setStatus(ItineraryStatus.COMPLETED);

        // Logic: If it was a draft, maybe set a completion date
        // trip.setCompletedAt(LocalDateTime.now());

        return itineraryMapper.toResponse(itineraryRepository.save(trip));
    }


    //Summary
    public ItinerarySummary calculateSummary(Itinerary itinerary) {
        ItinerarySummary summary = new ItinerarySummary();

        // 1. Total Budget (from the Itinerary header + any specific item costs)
        summary.setTotalEstimatedBudget(itinerary.getEstimatedBudget());

        if (itinerary.getItems() != null) {
            // 2. Count total vs completed
            summary.setActivityCount(itinerary.getItems().size());
            summary.setCompletedActivities(itinerary.getItems().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIsVisited()))
                    .count());

            // 3. Breakdown by type (Museum, Food, Trekking, etc.)
            Map<String, Long> breakdown = itinerary.getItems().stream()
                    .collect(Collectors.groupingBy(
                            item -> item.getActivityType() != null ? item.getActivityType() : "OTHER",
                            Collectors.counting()
                    ));
            summary.setActivityTypeBreakdown(breakdown);
        }

        return summary;
    }

    // ---- Helper Functions ----
    // 1. Rule: Must be Public and Completed (Tab 3)
    private Specification<Itinerary> isPublicAndCompleted() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isPublic"), true),
                cb.equal(root.get("status"), ItineraryStatus.COMPLETED)
        );
    }

    // 2. Rule: Match Theme
    private Specification<Itinerary> hasTheme(String themeName) {
        return (root, cq, cb) -> (themeName == null || themeName.isEmpty())
                ? null : cb.equal(root.get("theme"), themeName);
    }

    // 3. Rule: Match Budget Range
    private Specification<Itinerary> hasBudget(String budgetRange) {
        return (root, cq, cb) -> (budgetRange == null || budgetRange.isEmpty())
                ? null : cb.equal(root.get("budgetRange"), budgetRange);
    }

    // 4. Rule: Keyword Search (The one causing the error)
    private Specification<Itinerary> matchesSearch(String keyword) {
        return (root, cq, cb) -> {
            if (keyword == null || keyword.isEmpty()) return null;

            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}