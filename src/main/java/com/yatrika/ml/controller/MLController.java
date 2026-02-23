package com.yatrika.ml.controller;

import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.mapper.ItineraryMapper;
import com.yatrika.ml.dto.request.MLPredictRequest;
import com.yatrika.ml.dto.request.MLSaveRequest;
import com.yatrika.ml.dto.response.MLPredictResponse;
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

@RestController
@RequestMapping("/api/v1/ml")
@RequiredArgsConstructor
@Tag(name = "AI Trip Planner", description = "Endpoints for ML-generated itineraries")
public class MLController {

    private final MLItineraryService mlItineraryService;
    private final ItineraryMapper itineraryMapper;

    /**
     * Fetch ML itinerary preview from FastAPI.
     * Returns structured response (MLPredictResponse) for frontend consumption.
     */
    @PostMapping("/predict")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Get AI itinerary preview (FastAPI bridge)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MLPredictResponse> getPreview(@Valid @RequestBody MLPredictRequest request) {
        MLPredictResponse response = mlItineraryService.getPredictionFromFastAPI(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Save the AI itinerary to user's database plans",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> saveToMyPlans(
            @Valid @RequestBody MLSaveRequest saveRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Pass the new fields to the service
        var savedItinerary = mlItineraryService.saveMLPlan(
                saveRequest.getCity(),
                saveRequest.getDays(),
                saveRequest.getStartDate(),
                saveRequest.getInterests(),
                saveRequest.getItineraryData(), // Map<Integer, List<String>>
                principal.getId()
        );

        ItineraryResponse response = itineraryMapper.toResponse(savedItinerary, principal.getId());
        return ResponseEntity.ok(response);
    }
}


//package com.yatrika.ml.controller;
//
//import com.yatrika.itinerary.dto.response.ItineraryResponse;
//import com.yatrika.itinerary.mapper.ItineraryMapper;
//import com.yatrika.ml.dto.request.MLPredictRequest;
//import com.yatrika.ml.service.MLItineraryService;
//import com.yatrika.shared.security.UserPrincipal;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/v1/ml")
//@RequiredArgsConstructor
//@Tag(name = "AI Trip Planner", description = "Endpoints for ML-generated itineraries")
//public class MLController {
//
//    private final MLItineraryService mlItineraryService;
//    private final ItineraryMapper itineraryMapper;
//
//    @PostMapping("/predict")
//    @PreAuthorize("hasRole('USER')")
//    @Operation(summary = "Get AI itinerary preview (FastAPI bridge)",
//            security = @SecurityRequirement(name = "bearerAuth"))
//    public ResponseEntity<Map<String, List<String>>> getPreview(@Valid @RequestBody MLPredictRequest request) {
//        // Calls FastAPI and returns the raw map to Flutter
//        return ResponseEntity.ok(mlItineraryService.getPredictionFromFastAPI(request));
//    }
//
//    @PostMapping("/save")
//    @PreAuthorize("hasRole('USER')")
//    @Operation(
//            summary = "Save the AI itinerary to user's database plans",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<ItineraryResponse> saveToMyPlans(
//            @RequestBody com.yatrika.ml.dto.request.MLSaveRequest saveRequest,
//            @AuthenticationPrincipal UserPrincipal principal) {
//
//        var savedItinerary = mlItineraryService.saveMLPlan(
//                saveRequest.getMetadata(),
//                saveRequest.getItineraryData(),
//                principal.getId(),
//                saveRequest.getMetadata().getStartDate()
//        );
//
//        return ResponseEntity.ok(itineraryMapper.toResponse(savedItinerary, principal.getId()));
//    }
//}