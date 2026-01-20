package com.yatrika.review.dto.response;

import com.yatrika.user.dto.response.UserResponse;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private UserResponse user;
    private Long destinationId;
    private Integer rating;
    private String comment;
    private LocalDate visitedDate;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}