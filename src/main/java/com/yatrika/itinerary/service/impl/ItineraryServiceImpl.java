package com.yatrika.itinerary.service.impl;

import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.itinerary.domain.Itinerary;
import com.yatrika.itinerary.domain.ItineraryItem;
import com.yatrika.itinerary.domain.ItineraryStatus;
import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.mapper.ItineraryMapper;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.itinerary.service.ItineraryService;
import com.yatrika.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryMapper itineraryMapper;
    private final DestinationRepository destinationRepository;

    // ================= 1. DISCOVERY & EXPLORATION =================

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryResponse> getAdminTemplates() {
        return itineraryRepository.findByStatusAndIsAdminCreatedTrue(ItineraryStatus.TEMPLATE)
                .stream().map(itineraryMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getPublicCommunityTrips(Pageable pageable) {
        return itineraryRepository.findByStatusAndIsPublicTrueAndIsAdminCreatedFalse(
                ItineraryStatus.COMPLETED, pageable).map(itineraryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryResponse getItineraryById(Long id) {
        return itineraryRepository.findById(id)
                .map(itineraryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> searchPublicItineraries(ItineraryFilterRequest filter, Pageable pageable) {
        Specification<Itinerary> spec = Specification.where(isPublicAndCompleted())
                .and(hasTheme(filter.getTheme()))
                .and(matchesSearch(filter.getSearchQuery()));

        return itineraryRepository.findAll(spec, pageable).map(itineraryMapper::toResponse);
    }

    // ================= 2. LIFECYCLE (CREATE & COPY) =================

    @Override
    public ItineraryResponse createEmptyTrip(ItineraryRequest request, Long userId) {
        Itinerary newTrip = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(userId)
                .theme(request.getTheme())
                .status(ItineraryStatus.DRAFT)
                .isPublic(false)
                .isAdminCreated(false)
                .build();
        return itineraryMapper.toResponse(itineraryRepository.save(newTrip));
    }

    @Override
    public ItineraryResponse copyItinerary(Long originalId, Long currentUserId) {
        Itinerary original = itineraryRepository.findById(originalId)
                .orElseThrow(() -> new ResourceNotFoundException("Original Itinerary not found"));

        Itinerary clone = Itinerary.builder()
                .title(original.getTitle() + " (Copy)")
                .description(original.getDescription())
                .userId(currentUserId)
                .sourceId(original.getId())
                .status(ItineraryStatus.DRAFT)
                .isAdminCreated(false)
                .isPublic(false)
                .totalDays(original.getTotalDays())
                .theme(original.getTheme())
                .estimatedBudget(original.getEstimatedBudget())
                .items(new ArrayList<>())
                .build();

        // Deep copy items to ensure the new user copy is independent
        original.getItems().forEach(item -> {
            ItineraryItem newItem = ItineraryItem.builder()
                    .itinerary(clone)
                    .destination(item.getDestination())
                    .dayNumber(item.getDayNumber())
                    .orderInDay(item.getOrderInDay())
                    .title(item.getTitle())
                    .notes(item.getNotes())
                    .activityType(item.getActivityType())
                    .isVisited(false)
                    .build();
            clone.addItem(newItem);
        });

        original.setCopyCount(original.getCopyCount() + 1);
        return itineraryMapper.toResponse(itineraryRepository.save(clone));
    }

    // ================= 3. PERSONAL MANAGEMENT =================

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getMyItineraries(Long userId, Pageable pageable) {
        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(itineraryMapper::toResponse);
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
        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
    }

    // ================= 4. ITEM & ACTIVITY MANAGEMENT =================

    @Override
    public ItineraryResponse addItemToItinerary(Long itineraryId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);

        ItineraryItem newItem = ItineraryItem.builder()
                .itinerary(itinerary)
                .destination(destinationRepository.getReferenceById(request.getDestinationId()))
                .dayNumber(request.getDayNumber())
                .orderInDay(request.getOrderInDay())
                .title(request.getTitle())
                .notes(request.getNotes())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .activityType(request.getActivityType())
                .isVisited(false)
                .build();

        itinerary.addItem(newItem);
        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
    }

    @Override
    public ItineraryResponse updateItem(Long itineraryId, Long itemId, ItineraryItemRequest request, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);

        ItineraryItem item = itinerary.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Item not found in this trip"));

        item.setTitle(request.getTitle());
        item.setNotes(request.getNotes());
        item.setDayNumber(request.getDayNumber());
        item.setOrderInDay(request.getOrderInDay());
        item.setActivityType(request.getActivityType());
        item.setStartTime(request.getStartTime());
        item.setEndTime(request.getEndTime());

        return itineraryMapper.toResponse(itineraryRepository.save(itinerary));
    }

    @Override
    public void toggleItemVisited(Long itineraryId, Long itemId, Boolean visited, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        itinerary.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> item.setIsVisited(visited));
    }

    @Override
    public void reorderItems(Long itineraryId, List<Long> itemIdsInOrder, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        for (int i = 0; i < itemIdsInOrder.size(); i++) {
            Long currentId = itemIdsInOrder.get(i);
            int newOrder = i + 1;
            itinerary.getItems().stream()
                    .filter(item -> item.getId().equals(currentId))
                    .findFirst()
                    .ifPresent(item -> item.setOrderInDay(newOrder));
        }
    }

    @Override
    public void removeItem(Long itineraryId, Long itemId, Long userId) {
        Itinerary itinerary = getOwnedItinerary(itineraryId, userId);
        itinerary.getItems().removeIf(item -> item.getId().equals(itemId));
        itineraryRepository.save(itinerary);
    }

    // ================= 5. FINALIZING & SHARING =================

    @Override
    public ItineraryResponse completeTrip(Long id, Long userId) {
        Itinerary trip = getOwnedItinerary(id, userId);
        if (trip.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot complete an empty itinerary.");
        }
        trip.setStatus(ItineraryStatus.COMPLETED);
        return itineraryMapper.toResponse(itineraryRepository.save(trip));
    }

    @Override
    public ItineraryResponse shareTrip(Long id, Long userId) {
        Itinerary trip = getOwnedItinerary(id, userId);

        if (trip.getSourceId() != null) {
            throw new IllegalStateException("Copied itineraries cannot be shared to the public community tab.");
        }
        if (trip.getStatus() != ItineraryStatus.COMPLETED) {
            throw new IllegalStateException("Only completed original trips can be shared.");
        }

        trip.setIsPublic(true);
        return itineraryMapper.toResponse(itineraryRepository.save(trip));
    }

    // ================= 6. ADMIN SPECIFIC =================

    @Override
    public ItineraryResponse createAdminTemplate(ItineraryRequest request) {
        Itinerary template = Itinerary.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(null)
                .status(ItineraryStatus.TEMPLATE)
                .isAdminCreated(true)
                .isPublic(true)
                .theme(request.getTheme())
                .totalDays(request.getTotalDays())
                .build();
        return itineraryMapper.toResponse(itineraryRepository.save(template));
    }

    @Override
    public ItineraryResponse addItemToTemplate(Long templateId, ItineraryItemRequest request) {
        Itinerary template = itineraryRepository.findById(templateId)
                .filter(it -> it.getStatus() == ItineraryStatus.TEMPLATE)
                .orElseThrow(() -> new EntityNotFoundException("Admin Template not found"));

        ItineraryItem newItem = ItineraryItem.builder()
                .itinerary(template)
                .destination(destinationRepository.getReferenceById(request.getDestinationId()))
                .dayNumber(request.getDayNumber())
                .orderInDay(request.getOrderInDay())
                .title(request.getTitle())
                .activityType(request.getActivityType())
                .build();

        template.addItem(newItem);
        return itineraryMapper.toResponse(itineraryRepository.save(template));
    }

    // ================= PRIVATE HELPERS & SPECS =================

    private Itinerary getOwnedItinerary(Long id, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));
        if (itinerary.getUserId() == null || !itinerary.getUserId().equals(userId)) {
            throw new RuntimeException("Access Denied: You do not own this itinerary.");
        }
        return itinerary;
    }

    private Specification<Itinerary> isPublicAndCompleted() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isPublic"), true),
                cb.equal(root.get("status"), ItineraryStatus.COMPLETED)
        );
    }

    private Specification<Itinerary> hasTheme(String theme) {
        return (root, cq, cb) -> (theme == null || theme.isEmpty()) ? null : cb.equal(root.get("theme"), theme);
    }

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