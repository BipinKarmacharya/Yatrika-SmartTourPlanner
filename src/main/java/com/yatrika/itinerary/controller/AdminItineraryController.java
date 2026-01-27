package com.yatrika.itinerary.controller;

import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/itineraries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Itinerary Management", description = "Endpoints for creating expert curated templates")
public class AdminItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping
    @Operation(
            summary = "Create an admin itinerary template (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> createTemplate(@RequestBody ItineraryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.createAdminTemplate(request));
    }

    @PostMapping("/{id}/items")
    @Operation(
            summary = "Create an admin itinerary items template (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> addItem(@PathVariable Long id, @RequestBody ItineraryItemRequest request) {
        return ResponseEntity.ok(itineraryService.addItemToTemplate(id, request));
    }
}