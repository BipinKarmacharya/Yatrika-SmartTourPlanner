package com.yatrika.ml.controller;

import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.mapper.ItineraryMapper;
import com.yatrika.ml.dto.request.MLPredictRequest;
import com.yatrika.ml.service.MLItineraryService;
import com.yatrika.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ml")
@RequiredArgsConstructor
@Tag(name = "AI Trip Planner", description = "Endpoints for ML-generated itineraries")
public class MLController {

    private final MLItineraryService mlItineraryService;
    private final ItineraryMapper itineraryMapper;

    @PostMapping("/predict")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get AI itinerary preview (FastAPI bridge)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, List<String>>> getPreview(@Valid @RequestBody MLPredictRequest request) {
        // Calls FastAPI and returns the raw map to Flutter
        return ResponseEntity.ok(mlItineraryService.getPredictionFromFastAPI(request));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Save the AI itinerary to user's database plans",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> saveToMyPlans(
            @RequestBody com.yatrika.ml.dto.request.MLSaveRequest saveRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        var savedItinerary = mlItineraryService.saveMLPlan(
                saveRequest.getMetadata(),
                saveRequest.getItineraryData(),
                principal.getId()
        );

        return ResponseEntity.ok(itineraryMapper.toResponse(savedItinerary, principal.getId()));
    }
}