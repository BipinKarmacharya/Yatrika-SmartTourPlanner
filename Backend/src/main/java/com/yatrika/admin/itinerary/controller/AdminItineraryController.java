package com.yatrika.admin.itinerary.controller;

import com.yatrika.admin.itinerary.dto.request.AdminItineraryCreateRequest;
import com.yatrika.admin.itinerary.dto.request.AdminItineraryItemUpdateRequest;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryItemResponse;
import com.yatrika.admin.itinerary.dto.response.AdminItineraryResponse;
import com.yatrika.admin.itinerary.service.AdminItineraryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/itineraries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // All endpoints secured
@SecurityRequirement(name = "bearerAuth")
public class AdminItineraryController {

    private final AdminItineraryService itineraryService;

    // ================= CREATE =================
    @PostMapping
    public ResponseEntity<AdminItineraryResponse> createItinerary(
            @RequestBody @Valid AdminItineraryCreateRequest request) {
        return new ResponseEntity<>(itineraryService.createItinerary(request), HttpStatus.CREATED);
    }

    // ================= READ =================
    @GetMapping
    public ResponseEntity<List<AdminItineraryResponse>> getAllItineraries() {
        return ResponseEntity.ok(itineraryService.getAllActiveItineraries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminItineraryResponse> getItineraryById(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.getItineraryById(id));
    }

    // ================= UPDATE ITINERARY =================
    @PutMapping("/{id}")
    public ResponseEntity<AdminItineraryResponse> updateItinerary(
            @PathVariable Long id,
            @RequestBody @Valid AdminItineraryCreateRequest request) {
        return ResponseEntity.ok(itineraryService.updateItinerary(id, request));
    }

    // ================= DELETE ITINERARY =================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItinerary(@PathVariable Long id) {
        itineraryService.softDeleteItinerary(id);
        return ResponseEntity.noContent().build();
    }

    // ================= UPDATE ITEM =================
    @PutMapping("/items/{itemId}")
    public ResponseEntity<AdminItineraryItemResponse> updateItem(
            @PathVariable Long itemId,
            @RequestBody @Valid AdminItineraryItemUpdateRequest request) {
        return ResponseEntity.ok(itineraryService.updateItem(itemId, request));
    }

    // ================= DELETE ITEM =================
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<AdminItineraryItemResponse> deleteItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(itineraryService.deleteItem(itemId));
    }
}
