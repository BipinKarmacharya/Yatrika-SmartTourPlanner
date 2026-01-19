package com.yatrika.user.itinerary.controller;

import com.yatrika.user.itinerary.dto.request.UserItineraryCreateRequest;
import com.yatrika.user.itinerary.dto.response.UserItineraryResponse;
import com.yatrika.user.itinerary.service.UserItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/itineraries")
@RequiredArgsConstructor
public class UserItineraryController {

    private final UserItineraryService itineraryService;

    // ================= CREATE =================
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<UserItineraryResponse> createItinerary(
            @RequestBody @Valid UserItineraryCreateRequest request
    ) {
        UserItineraryResponse response = itineraryService.createItinerary(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ================= READ ALL =================
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserItineraryResponse>> getAllUserItineraries(
            @PathVariable Long userId
    ) {
        List<UserItineraryResponse> itineraries = itineraryService.getAllUserItineraries(userId);
        return ResponseEntity.ok(itineraries);
    }

    // ================= READ SINGLE =================
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}")
    public ResponseEntity<UserItineraryResponse> getItineraryById(@PathVariable Long id) {
        UserItineraryResponse response = itineraryService.getItineraryById(id);
        return ResponseEntity.ok(response);
    }

    // ================= UPDATE =================
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    public ResponseEntity<UserItineraryResponse> updateItinerary(
            @PathVariable Long id,
            @RequestBody @Valid UserItineraryCreateRequest request
    ) {
        UserItineraryResponse response = itineraryService.updateItinerary(id, request);
        return ResponseEntity.ok(response);
    }

    // ================= SOFT DELETE =================
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItinerary(@PathVariable Long id) {
        itineraryService.softDeleteItinerary(id);
        return ResponseEntity.noContent().build();
    }

    // ================= UPDATE ITEM =================
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<UserItineraryResponse> updateItineraryItem(
            @PathVariable Long itemId,
            @RequestBody @Valid UserItineraryCreateRequest itemRequest
    ) {
        UserItineraryResponse response = itineraryService.updateItineraryItem(itemId, itemRequest);
        return ResponseEntity.ok(response);
    }

    // ================= DELETE ITEM =================
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<UserItineraryResponse> deleteItineraryItem(@PathVariable Long itemId) {
        UserItineraryResponse response = itineraryService.deleteItineraryItem(itemId);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Copy an Admin itinerary to User itinerary", description = "User can select an Admin itinerary as a template and create their own trip")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/copy/{adminItineraryId}")
    public ResponseEntity<UserItineraryResponse> copyAdminItinerary(
            @PathVariable Long adminItineraryId,
            @RequestParam Long userId // or get from JWT token in production
    ) {
        UserItineraryResponse response = itineraryService.copyAdminItinerary(adminItineraryId, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
