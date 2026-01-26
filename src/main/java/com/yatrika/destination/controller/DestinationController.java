package com.yatrika.destination.controller;

import com.yatrika.destination.dto.request.DestinationRequest;
import com.yatrika.destination.dto.request.DestinationSearchRequest;
import com.yatrika.destination.dto.request.NearbySearchRequest;
import com.yatrika.destination.dto.response.BulkDestinationResult;
import com.yatrika.destination.dto.response.DestinationResponse;
import com.yatrika.destination.service.DestinationService;
import com.yatrika.user.service.CurrentUserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
@Tag(name = "Destinations", description = "Destination management APIs")
public class DestinationController {

    private final DestinationService destinationService;
    private final CurrentUserService currentUserService;

    // --- 🔓 PUBLIC ENDPOINTS (Guest Access) ---

    @GetMapping("/{id}")
    @Operation(summary = "Get destination by ID (Public)")
    public ResponseEntity<DestinationResponse> getDestination(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationById(id));
    }

    @GetMapping
    @Operation(summary = "Get all destinations with pagination (Public)")
    public ResponseEntity<Page<DestinationResponse>> getAllDestinations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(destinationService.getAllDestinations(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search destinations with filters (Public)")
    public ResponseEntity<Page<DestinationResponse>> searchDestinations(
            @ModelAttribute DestinationSearchRequest request) {
        return ResponseEntity.ok(destinationService.searchDestinations(request));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby destinations (Public)")
    public ResponseEntity<List<DestinationResponse>> findNearbyDestinations(
            @ModelAttribute @Valid NearbySearchRequest request) {
        return ResponseEntity.ok(destinationService.findNearbyDestinations(request));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular destinations (Public)")
    public ResponseEntity<Page<DestinationResponse>> getPopularDestinations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("popularityScore").descending());
        return ResponseEntity.ok(destinationService.getPopularDestinations(pageable));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()") // Only logged-in users get personalized recs
    @Operation(
            summary = "Get personalized recommendations based on user interests",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<DestinationResponse>> getRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Use your security service to get the current user's ID
        Long currentUserId = currentUserService.getCurrentUserId();

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(destinationService.getRecommendationsForUser(currentUserId, pageable));
    }

    // --- 🔒 ADMIN ENDPOINTS ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new destination (Admin only)",
            description = "The image URLs must be obtained first via /api/uploads/destination",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<DestinationResponse> createDestination(
            @Valid @RequestBody DestinationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(destinationService.createDestination(request));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Bulk create destinations (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<BulkDestinationResult> bulkCreateDestinations(
            @Valid @RequestBody List<DestinationRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(destinationService.bulkCreate(requests));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update destination (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<DestinationResponse> updateDestination(
            @PathVariable Long id,
            @Valid @RequestBody DestinationRequest request) {
        return ResponseEntity.ok(destinationService.updateDestination(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete destination (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteDestination(@PathVariable Long id) {
        destinationService.deleteDestination(id);
        return ResponseEntity.noContent().build();
    }
}