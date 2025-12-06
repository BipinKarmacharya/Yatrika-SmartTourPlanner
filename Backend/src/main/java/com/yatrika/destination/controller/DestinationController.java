// Update @PreAuthorize annotations in DestinationController
package com.yatrika.destination.controller;

import com.yatrika.destination.dto.request.DestinationRequest;
import com.yatrika.destination.dto.request.DestinationSearchRequest;
import com.yatrika.destination.dto.request.NearbySearchRequest;
import com.yatrika.destination.dto.response.DestinationResponse;
import com.yatrika.destination.service.DestinationService;
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

    // 🔓 PUBLIC ENDPOINTS (Guest Access)

    @GetMapping("/{id}")
    @Operation(summary = "Get destination by ID (Public)")
    public ResponseEntity<DestinationResponse> getDestination(@PathVariable Long id) {
        DestinationResponse response = destinationService.getDestinationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all destinations with pagination (Public)")
    public ResponseEntity<Page<DestinationResponse>> getAllDestinations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<DestinationResponse> response = destinationService.getAllDestinations(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search destinations with filters (Public)")
    public ResponseEntity<Page<DestinationResponse>> searchDestinations(
            @ModelAttribute DestinationSearchRequest request) {
        Page<DestinationResponse> response = destinationService.searchDestinations(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby destinations (Public)")
    public ResponseEntity<List<DestinationResponse>> findNearbyDestinations(
            @ModelAttribute @Valid NearbySearchRequest request) {
        List<DestinationResponse> response = destinationService.findNearbyDestinations(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular destinations (Public)")
    public ResponseEntity<Page<DestinationResponse>> getPopularDestinations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("popularityScore").descending());
        Page<DestinationResponse> response = destinationService.getPopularDestinations(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/district/{district}")
    @Operation(summary = "Get destinations by district (Public)")
    public ResponseEntity<Page<DestinationResponse>> getDestinationsByDistrict(
            @PathVariable String district,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<DestinationResponse> response = destinationService.getDestinationsByDistrict(district, pageable);
        return ResponseEntity.ok(response);
    }

    // 🔒 AUTHENTICATED ENDPOINTS

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new destination (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<DestinationResponse> createDestination(
            @Valid @RequestBody DestinationRequest request) {
        DestinationResponse response = destinationService.createDestination(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        DestinationResponse response = destinationService.updateDestination(id, request);
        return ResponseEntity.ok(response);
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