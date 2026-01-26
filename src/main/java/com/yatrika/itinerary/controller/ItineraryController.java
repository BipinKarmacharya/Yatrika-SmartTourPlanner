package com.yatrika.itinerary.controller;

import com.yatrika.itinerary.dto.request.ItineraryFilterRequest;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
@Tag(name = "Itinerary Management", description = "APIs for expert templates, community trips, and personal planning")
public class ItineraryController {

    private final ItineraryService itineraryService;

    // ================= EXPLORE TABS =================

    @GetMapping("/admin-templates")
    @Operation(summary = "Tab 2: Get expert-curated admin itineraries")
    public ResponseEntity<List<ItineraryResponse>> getAdminTemplates() {
        return ResponseEntity.ok(itineraryService.getAdminTemplates());
    }

    @GetMapping("/community")
    @Operation(summary = "Tab 3: Get public trips shared by other users")
    public ResponseEntity<Page<ItineraryResponse>> getPublicTrips(Pageable pageable) {
        return ResponseEntity.ok(itineraryService.getPublicCommunityTrips(pageable));
    }

    // ================= ACTIONS (COPY/CREATE) =================

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Copy an Admin template or Public trip to my own plans")
    public ResponseEntity<ItineraryResponse> copy(@PathVariable Long id) {
        // In a real app, extract userId from SecurityContext/JWT
        Long currentUserId = 1L; // Placeholder for logic
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.copyItinerary(id, currentUserId));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a blank trip from scratch")
    public ResponseEntity<ItineraryResponse> create(@RequestBody ItineraryRequest request) {
        Long currentUserId = 1L; // Placeholder
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.createEmptyTrip(request, currentUserId));
    }

    // ================= SOCIAL ACTIONS =================

    @PatchMapping("/{id}/share")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Make a completed trip public (Cannot share copied trips)")
    public ResponseEntity<ItineraryResponse> share(@PathVariable Long id) {
        Long currentUserId = 1L; // Placeholder
        return ResponseEntity.ok(itineraryService.shareTrip(id, currentUserId));
    }


    @GetMapping("/my-plans")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user's personal itineraries")
    public ResponseEntity<Page<ItineraryResponse>> getMyPlans(Pageable pageable) {
        Long currentUserId = 1L; // Placeholder for JWT logic
        return ResponseEntity.ok(itineraryService.getMyItineraries(currentUserId, pageable));
    }

    @PutMapping("/{itineraryId}/items/{itemId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a specific activity in a personal itinerary")
    public ResponseEntity<ItineraryResponse> updateActivity(
            @PathVariable Long itineraryId,
            @PathVariable Long itemId,
            @RequestBody ItineraryItemRequest request) {
        Long currentUserId = 1L;
        return ResponseEntity.ok(itineraryService.updateItem(itineraryId, itemId, request, currentUserId));
    }

    @DeleteMapping("/{itineraryId}/items/{itemId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Remove an activity from a personal itinerary")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long itineraryId,
            @PathVariable Long itemId) {
        Long currentUserId = 1L;
        itineraryService.removeItem(itineraryId, itemId, currentUserId);
        return ResponseEntity.noContent().build();
    }


    // --- Search
    @GetMapping("/search")
    @Operation(summary = "Search and filter public itineraries")
    public ResponseEntity<Page<ItineraryResponse>> search(
            ItineraryFilterRequest filter,
            Pageable pageable) {
        return ResponseEntity.ok(itineraryService.searchPublicItineraries(filter, pageable));
    }


    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark a trip as finished")
    public ResponseEntity<ItineraryResponse> complete(@PathVariable Long id) {
        Long currentUserId = 1L; // Placeholder
        return ResponseEntity.ok(itineraryService.completeTrip(id, currentUserId));
    }
}