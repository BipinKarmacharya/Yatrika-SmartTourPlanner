package com.yatrika.itinerary.controller;

import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.service.ItineraryService;
import com.yatrika.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
@Tag(name = "Itinerary Management", description = "APIs organized by Discovery, Lifecycle, and Personal Management")
public class ItineraryController {

    private final ItineraryService itineraryService;

    // Helper to get the ID from the JWT token
    private Long getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }

    // ================= STEP 1: DISCOVERY & EXPLORATION =================
    // (Open to everyone or users exploring templates)

    @GetMapping("/admin-templates")
    @Operation(summary = "Get expert-curated admin itineraries (Tab 2)")
    public ResponseEntity<List<ItineraryResponse>> getAdminTemplates() {
        return ResponseEntity.ok(itineraryService.getAdminTemplates());
    }

    @GetMapping("/community")
    @Operation(summary = "Get public trips shared by other users (Tab 3)")
    public ResponseEntity<Page<ItineraryResponse>> getPublicTrips(Pageable pageable) {
        return ResponseEntity.ok(itineraryService.getPublicCommunityTrips(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter public itineraries")
    public ResponseEntity<Page<ItineraryResponse>> search(
            ItineraryFilterRequest filter,
            Pageable pageable) {
        return ResponseEntity.ok(itineraryService.searchPublicItineraries(filter, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full details of any itinerary by ID")
    public ResponseEntity<ItineraryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.getItineraryById(id));
    }

    // ================= STEP 2: LIFECYCLE (CREATE & COPY) =================
    // (Moving from discovery to ownership)

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a blank trip from scratch")
    public ResponseEntity<ItineraryResponse> create(@RequestBody ItineraryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.createEmptyTrip(request, getCurrentUserId()));
    }

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Copy an Admin template or Public trip to your own plans")
    public ResponseEntity<ItineraryResponse> copy(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.copyItinerary(id, getCurrentUserId()));
    }

    // ================= STEP 3: PERSONAL MANAGEMENT (MY TRIPS) =================
    // (Managing your owned independent copies)

    @GetMapping("/my-plans")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user's personal itineraries")
    public ResponseEntity<Page<ItineraryResponse>> getMyPlans(Pageable pageable) {
        return ResponseEntity.ok(itineraryService.getMyItineraries(getCurrentUserId(), pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update itinerary header (Title, dates, etc)")
    public ResponseEntity<ItineraryResponse> updateHeader(
            @PathVariable Long id,
            @RequestBody ItineraryRequest request) {
        return ResponseEntity.ok(itineraryService.updateItineraryHeader(id, request, getCurrentUserId()));
    }

    // ================= STEP 4: ITEM & PROGRESS MANAGEMENT =================
    // (Day-by-day actions)

    @PostMapping("/{itineraryId}/items")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Add a new destination/activity to your plan")
    public ResponseEntity<ItineraryResponse> addItem(
            @PathVariable Long itineraryId,
            @RequestBody ItineraryItemRequest request) {
        return ResponseEntity.ok(itineraryService.addItemToItinerary(itineraryId, request, getCurrentUserId()));
    }

    @PutMapping("/{itineraryId}/items/{itemId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a specific activity details")
    public ResponseEntity<ItineraryResponse> updateActivity(
            @PathVariable Long itineraryId,
            @PathVariable Long itemId,
            @RequestBody ItineraryItemRequest request) {
        return ResponseEntity.ok(itineraryService.updateItem(itineraryId, itemId, request, getCurrentUserId()));
    }

    @PatchMapping("/{itineraryId}/items/{itemId}/toggle-visited")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark a specific destination as visited")
    public ResponseEntity<Void> toggleVisited(
            @PathVariable Long itineraryId,
            @PathVariable Long itemId,
            @RequestParam Boolean visited) {
        itineraryService.toggleItemVisited(itineraryId, itemId, visited, getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/items/reorder")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Reorder activities within your trip")
    public ResponseEntity<Void> reorderItems(
            @PathVariable Long id,
            @RequestBody List<Long> itemIdsInOrder) {
        itineraryService.reorderItems(id, itemIdsInOrder, getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{itineraryId}/items/{itemId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Remove an activity from your plan")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long itineraryId,
            @PathVariable Long itemId) {
        itineraryService.removeItem(itineraryId, itemId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    // ================= STEP 5: FINALIZING & SHARING =================
    // (Closing the loop)

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark a trip as finished")
    public ResponseEntity<ItineraryResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.completeTrip(id, getCurrentUserId()));
    }

    @PatchMapping("/{id}/share")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Make your completed original trip public")
    public ResponseEntity<ItineraryResponse> share(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.shareTrip(id, getCurrentUserId()));
    }
}