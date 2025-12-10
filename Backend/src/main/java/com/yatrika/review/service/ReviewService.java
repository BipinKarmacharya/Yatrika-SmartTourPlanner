package com.yatrika.review.service;

import com.yatrika.destination.domain.Destination;
import com.yatrika.destination.repository.DestinationRepository;
import com.yatrika.review.domain.Review;
import com.yatrika.review.dto.request.ReviewRequest;
import com.yatrika.review.dto.response.ReviewResponse;
import com.yatrika.review.mapper.ReviewMapper;
import com.yatrika.review.repository.ReviewRepository;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.user.domain.User;
import com.yatrika.user.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DestinationRepository destinationRepository;
    private final AuthService authService;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        Destination destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId()));

        // Check if user already reviewed this destination
        if (reviewRepository.existsByUserIdAndDestinationId(currentUser.getId(), request.getDestinationId())) {
            throw new AppException("You have already reviewed this destination");
        }

        Review review = Review.builder()
                .user(currentUser)
                .destination(destination)
                .rating(request.getRating())
                .comment(request.getComment())
                .visitedDate(request.getVisitedDate())
                .isVerified(false)
                .build();

        Review savedReview = reviewRepository.save(review);

        // Update destination's average rating
        updateDestinationRating(destination.getId());

        log.info("Review created by user {} for destination {}", currentUser.getId(), destination.getId());
        return reviewMapper.toResponse(savedReview);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User currentUser = authService.getCurrentUserEntity();
        if (!review.isByUser(currentUser.getId())) {
            throw new AppException("You can only update your own reviews");
        }

        // If destination changed, check if user already reviewed the new destination
        if (!review.getDestination().getId().equals(request.getDestinationId())) {
            if (reviewRepository.existsByUserIdAndDestinationId(currentUser.getId(), request.getDestinationId())) {
                throw new AppException("You have already reviewed this destination");
            }
            Destination newDestination = destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", request.getDestinationId()));
            review.setDestination(newDestination);
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setVisitedDate(request.getVisitedDate());

        Review updatedReview = reviewRepository.save(review);

        // Update both old and new destination ratings
        if (!review.getDestination().getId().equals(request.getDestinationId())) {
            updateDestinationRating(review.getDestination().getId());
        }
        updateDestinationRating(request.getDestinationId());

        return reviewMapper.toResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User currentUser = authService.getCurrentUserEntity();
        if (!review.isByUser(currentUser.getId())) {
            throw new AppException("You can only delete your own reviews");
        }

        Long destinationId = review.getDestination().getId();
        reviewRepository.delete(review);

        // Update destination's average rating
        updateDestinationRating(destinationId);
    }

    public Page<ReviewResponse> getReviewsByDestination(Long destinationId, Pageable pageable) {
        if (!destinationRepository.existsById(destinationId)) {
            throw new ResourceNotFoundException("Destination", "id", destinationId);
        }

        Page<Review> reviews = reviewRepository.findByDestinationId(destinationId, pageable);
        return reviews.map(reviewMapper::toResponse);
    }

    public Page<ReviewResponse> getMyReviews(Pageable pageable) {
        User currentUser = authService.getCurrentUserEntity();
        Page<Review> reviews = reviewRepository.findByUserId(currentUser.getId(), pageable);
        return reviews.map(reviewMapper::toResponse);
    }

    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        return reviewMapper.toResponse(review);
    }

    // Admin method to verify a review
    @Transactional
    public ReviewResponse verifyReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setIsVerified(true);
        Review verifiedReview = reviewRepository.save(review);

        return reviewMapper.toResponse(verifiedReview);
    }

    private void updateDestinationRating(Long destinationId) {
        Double averageRating = reviewRepository.findAverageRatingByDestinationId(destinationId);
        Long totalReviews = reviewRepository.countByDestinationId(destinationId);

        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", "id", destinationId));

        if (averageRating != null) {
            destination.setAverageRating(BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP));
        } else {
            destination.setAverageRating(BigDecimal.ZERO);
        }
        destination.setTotalReviews(totalReviews.intValue());

        destinationRepository.save(destination);
    }
}