package com.yatrika.community.controller;

import com.yatrika.community.dto.request.CreatePostRequest;
import com.yatrika.community.dto.request.UpdatePostRequest;
import com.yatrika.community.dto.response.PostResponse;
import com.yatrika.community.service.PostService;
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
@RequestMapping("/api/community/posts")
@RequiredArgsConstructor
@Tag(name = "Community Posts", description = "Travel community posts APIs")
public class PostController {

    private final PostService postService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Create a new community post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Update a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        PostResponse response = postService.updatePost(id, request);
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
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> response = postService.searchPosts(q, pageable);
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

    @PostMapping("/{id}/like")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Like a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> likePost(@PathVariable Long id) {
        postService.likePost(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Unlike a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> unlikePost(@PathVariable Long id) {
        postService.unlikePost(id);
        return ResponseEntity.noContent().build();
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