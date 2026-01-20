package com.yatrika.itinerary.controller;

import com.yatrika.itinerary.domain.ItineraryStatus;
import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryItemResponse;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.service.ItineraryService;
import com.yatrika.shared.security.annotations.RequireUserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
@Tag(name = "Itineraries", description = "Trip planning APIs (User only)")
@RequireUserRole  // 🔐 ALL endpoints in this controller require USER role
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping
    @Operation(
            summary = "Create a new itinerary",
            description = "Only registered USERS can create itineraries",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> createItinerary(
            @Valid @RequestBody ItineraryRequest request) {
        ItineraryResponse response = itineraryService.createItinerary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(
            summary = "Get current user's itineraries",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<ItineraryResponse>> getMyItineraries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ItineraryResponse> response = itineraryService.getUserItineraries(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get itinerary by ID",
            description = "User can view own itineraries or public itineraries",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> getItinerary(@PathVariable Long id) {
        ItineraryResponse response = itineraryService.getItineraryById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update itinerary",
            description = "Only itinerary owner can update",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> updateItinerary(
            @PathVariable Long id,
            @Valid @RequestBody ItineraryRequest request) {
        ItineraryResponse response = itineraryService.updateItinerary(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete itinerary",
            description = "Only itinerary owner can delete",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    @Operation(
            summary = "Add destination to itinerary",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryItemResponse> addDestinationToItinerary(
            @PathVariable Long id,
            @Valid @RequestBody ItineraryItemRequest request) {
        ItineraryItemResponse response = itineraryService.addDestinationToItinerary(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status/{status}")
    @Operation(
            summary = "Change itinerary status",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> changeStatus(
            @PathVariable Long id,
            @PathVariable ItineraryStatus status) {
        ItineraryResponse response = itineraryService.changeStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    @Operation(
            summary = "Get public itineraries for inspiration",
            description = "Available to guests and users (excluding own itineraries)"
    )
    public ResponseEntity<Page<ItineraryResponse>> getPublicItineraries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("totalLikes").descending());
        Page<ItineraryResponse> response = itineraryService.getPublicItineraries(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/duplicate")
    @Operation(
            summary = "Duplicate a public itinerary",
            description = "User can duplicate any public itinerary to their account",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> duplicateItinerary(@PathVariable Long id) {
        // Implementation for duplicating public itineraries
        // TODO: Implement this method
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}