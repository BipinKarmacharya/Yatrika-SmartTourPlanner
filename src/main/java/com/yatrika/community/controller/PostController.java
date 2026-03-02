package com.yatrika.community.controller;

import com.yatrika.community.dto.request.CreatePostRequest;
import com.yatrika.community.dto.request.UpdatePostRequest;
import com.yatrika.community.dto.response.PostResponse;
import com.yatrika.community.service.impl.PostServiceImpl;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/community/posts")
@RequiredArgsConstructor
@Tag(name = "Community Posts", description = "Travel community posts APIs")
public class PostController {

    private final PostServiceImpl postService;

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new community post with image")
    public ResponseEntity<PostResponse> createPost(
            @RequestPart("data") @Valid CreatePostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        PostResponse response = postService.createPost(request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @RequestPart("data") @Valid UpdatePostRequest request,
            // Changed to List to support adding more images during update
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        PostResponse response = postService.updatePost(id, request, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Delete a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        PostResponse response = postService.getPostById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Get current user's posts",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<PostResponse>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<PostResponse> response = postService.getMyPosts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public community posts")
    public ResponseEntity<Page<PostResponse>> getPublicPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> response = postService.getPublicPosts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search public posts")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> response = postService.searchPosts(query, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending posts")
    public ResponseEntity<Page<PostResponse>> getTrendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> response = postService.getTrendingPosts(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/like/toggle")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Toggle like/unlike on a post")
    public ResponseEntity<PostResponse> toggleLike(@PathVariable Long id) {
        return ResponseEntity.ok(postService.toggleLikePost(id));
    }

    @GetMapping("/user/{userId}/stats")
    @Operation(summary = "Get user's post statistics")
    public ResponseEntity<UserPostStats> getUserPostStats(@PathVariable Long userId) {
        Long postCount = postService.getUserPostCount(userId);
        Long likesCount = postService.getUserLikesCount(userId);

        UserPostStats stats = new UserPostStats(postCount, likesCount);
        return ResponseEntity.ok(stats);
    }

    // Inner class for stats response
    private record UserPostStats(Long postCount, Long totalLikesReceived) {}
}