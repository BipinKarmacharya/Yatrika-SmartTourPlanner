package com.yatrika.itinerary.service;

import com.yatrika.itinerary.domain.*;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryItemResponse;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.mapper.ItineraryMapper;
import com.yatrika.itinerary.repository.ItineraryRepository;
import com.yatrika.user.domain.User;
import com.yatrika.user.service.AuthService;
import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.service.DestinationService;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.shared.security.RolePermissionService;
import com.yatrika.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final CurrentUserService currentUserService;
    private final DestinationService destinationService;
    private final RolePermissionService rolePermissionService;
    private final ItineraryMapper itineraryMapper;

    // 🔐 Only USERS can create itineraries
    @Transactional
    public ItineraryResponse createItinerary(ItineraryRequest request) {
        log.info("Creating new itinerary: {}", request.getTitle());

        // Check if user has USER role
        if (!rolePermissionService.canCreateItinerary()) {
            throw new AppException("Only registered users can create itineraries. Please login or register.");
        }

        User currentUser = currentUserService.getCurrentUserEntity();

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException("End date must be after start date");
        }

        // Calculate total days
        long totalDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (totalDays > 90) {
            throw new AppException("Itinerary cannot exceed 90 days");
        }

        // Create itinerary
        Itinerary itinerary = Itinerary.builder()
                .user(currentUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays((int) totalDays)
                .tripType(request.getTripType())
                .budgetRange(request.getBudgetRange())
                .estimatedTotalCost(request.getEstimatedBudget())
                .status(ItineraryStatus.DRAFT)
                .isPublic(request.getIsPublic())
                .build();

        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        log.info("Itinerary created successfully: {} (ID: {})", savedItinerary.getTitle(), savedItinerary.getId());

        return itineraryMapper.toResponse(savedItinerary);
    }

    // Get user's own itineraries
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getUserItineraries(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUserEntity();
        log.debug("Fetching itineraries for user: {}", currentUser.getId());

        Page<Itinerary> itineraries = itineraryRepository.findByUserId(currentUser.getId(), pageable);
        return itineraries.map(itineraryMapper::toResponse);
    }

    // Get itinerary by ID (with ownership check)
    @Transactional(readOnly = true)
    public ItineraryResponse getItineraryById(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));

        // Check if user owns the itinerary or it's public
        User currentUser = currentUserService.getCurrentUserEntity();
        if (!itinerary.isOwner(currentUser) && !itinerary.getIsPublic()) {
            throw new AppException("You don't have permission to view this itinerary");
        }

        // Increment view count if not owner
        if (!itinerary.isOwner(currentUser)) {
            itinerary.setTotalViews(itinerary.getTotalViews() + 1);
            itineraryRepository.save(itinerary);
        }

        return itineraryMapper.toResponse(itinerary);
    }

    // Update itinerary (only owner)
    @Transactional
    public ItineraryResponse updateItinerary(Long id, ItineraryRequest request) {
        Itinerary itinerary = getItineraryAndCheckOwnership(id);

        // Update fields
        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setTripType(request.getTripType());
        itinerary.setBudgetRange(request.getBudgetRange());
        itinerary.setEstimatedTotalCost(request.getEstimatedBudget());
        itinerary.setIsPublic(request.getIsPublic());

        // Recalculate total days
        long totalDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        itinerary.setTotalDays((int) totalDays);

        Itinerary updatedItinerary = itineraryRepository.save(itinerary);
        log.info("Itinerary updated: {}", updatedItinerary.getTitle());

        return itineraryMapper.toResponse(updatedItinerary);
    }

    // Delete itinerary (only owner)
    @Transactional
    public void deleteItinerary(Long id) {
        Itinerary itinerary = getItineraryAndCheckOwnership(id);

        // Only allow deletion of DRAFT or CANCELLED itineraries
        if (itinerary.getStatus() == ItineraryStatus.IN_PROGRESS ||
                itinerary.getStatus() == ItineraryStatus.COMPLETED) {
            throw new AppException("Cannot delete itineraries that are in progress or completed");
        }

        itineraryRepository.delete(itinerary);
        log.info("Itinerary deleted: {}", itinerary.getTitle());
    }

    // Add destination to itinerary
    @Transactional
    public ItineraryItemResponse addDestinationToItinerary(Long itineraryId, ItineraryItemRequest request) {
        Itinerary itinerary = getItineraryAndCheckOwnership(itineraryId);

        // Check if itinerary is editable
        if (!itinerary.canEdit(itinerary.getUser())) {
            throw new AppException("Cannot modify itinerary with status: " + itinerary.getStatus());
        }

        // Get destination
        Destination destination = destinationService.getDestinationEntity(request.getDestinationId());

        // Check if day number is within itinerary range
        if (request.getDayNumber() > itinerary.getTotalDays()) {
            throw new AppException("Day number exceeds itinerary duration");
        }

        // Create itinerary item
        ItineraryItem item = ItineraryItem.builder()
                .itinerary(itinerary)
                .destination(destination)
                .dayNumber(request.getDayNumber())
                .orderInDay(request.getOrderInDay())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .activityType(request.getActivityType())
                .title(request.getTitle())
                .description(request.getDescription())
                .notes(request.getNotes())
                .estimatedCost(request.getEstimatedCost())
                .build();

        itinerary.addItem(item);
        itineraryRepository.save(itinerary);

        log.info("Destination added to itinerary: {} -> {}", destination.getName(), itinerary.getTitle());

        return itineraryMapper.toItemResponse(item);
    }

    // Change itinerary status
    @Transactional
    public ItineraryResponse changeStatus(Long id, ItineraryStatus newStatus) {
        Itinerary itinerary = getItineraryAndCheckOwnership(id);

        // Validate status transition
        validateStatusTransition(itinerary.getStatus(), newStatus);

        itinerary.setStatus(newStatus);

        // If marking as completed, calculate actual cost
        if (newStatus == ItineraryStatus.COMPLETED) {
            calculateActualCost(itinerary);
        }

        Itinerary updatedItinerary = itineraryRepository.save(itinerary);
        log.info("Itinerary status changed: {} -> {}", itinerary.getTitle(), newStatus);

        return itineraryMapper.toResponse(updatedItinerary);
    }

    // Get public itineraries for inspiration
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getPublicItineraries(Pageable pageable) {
        log.debug("Fetching public itineraries");

        User currentUser = currentUserService.getCurrentUserEntity();
        Page<Itinerary> itineraries;

        if (currentUser != null) {
            // Exclude user's own itineraries
            itineraries = itineraryRepository.findByIsPublicTrueAndStatusAndUserIdNot(
                    ItineraryStatus.COMPLETED, currentUser.getId(), pageable);
        } else {
            // Guest access
            itineraries = itineraryRepository.findByIsPublicTrueAndStatus(
                    ItineraryStatus.COMPLETED, pageable);
        }

        return itineraries.map(itineraryMapper::toResponse);
    }

    // Helper methods
    private Itinerary getItineraryAndCheckOwnership(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        User currentUser = currentUserService.getCurrentUserEntity();
        if (!itinerary.isOwner(currentUser)) {
            throw new AppException("You don't own this itinerary");
        }

        return itinerary;
    }

    private void validateStatusTransition(ItineraryStatus current, ItineraryStatus next) {
        // Simple validation - can be expanded
        if (current == ItineraryStatus.COMPLETED && next != ItineraryStatus.COMPLETED) {
            throw new AppException("Cannot change status from COMPLETED");
        }

        if (current == ItineraryStatus.CANCELLED && next != ItineraryStatus.CANCELLED) {
            throw new AppException("Cannot change status from CANCELLED");
        }
    }

    private void calculateActualCost(Itinerary itinerary) {
        BigDecimal totalActualCost = BigDecimal.ZERO;

        for (ItineraryItem item : itinerary.getItems()) {
            if (item.getActualCost() != null) {
                totalActualCost = totalActualCost.add(item.getActualCost());
            } else if (item.getEstimatedCost() != null) {
                totalActualCost = totalActualCost.add(item.getEstimatedCost());
            }
        }

        itinerary.setActualTotalCost(totalActualCost);
    }
}
