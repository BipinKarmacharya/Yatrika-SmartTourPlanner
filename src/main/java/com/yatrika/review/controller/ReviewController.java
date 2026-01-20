package com.yatrika.review.controller;

import com.yatrika.review.dto.request.ReviewRequest;
import com.yatrika.review.dto.response.ReviewResponse;
import com.yatrika.review.service.ReviewService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review management APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Create a new review",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Update review",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Delete review",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/destination/{destinationId}")
    @Operation(summary = "Get reviews for a destination")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByDestination(
            @PathVariable Long destinationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ReviewResponse> response = reviewService.getReviewsByDestination(destinationId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Get current user's reviews",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> response = reviewService.getMyReviews(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        ReviewResponse response = reviewService.getReviewById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Verify a review (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ReviewResponse> verifyReview(@PathVariable Long id) {
        ReviewResponse response = reviewService.verifyReview(id);
        return ResponseEntity.ok(response);
    }
}