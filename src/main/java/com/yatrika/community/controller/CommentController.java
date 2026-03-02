package com.yatrika.community.controller;

import com.yatrika.community.dto.request.CommentRequestDTO;
import com.yatrika.community.dto.response.CommentResponseDTO;
import com.yatrika.community.service.CommentService;
import com.yatrika.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Create a new comment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<CommentResponseDTO> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal, // 1. Change to UserPrincipal
            @RequestBody CommentRequestDTO request) {

        // 2. Pass the ID instead of the whole object
        return ResponseEntity.ok(commentService.addComment(postId, principal.getId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponseDTO>> getComments(
            @PathVariable Long postId,
            Pageable pageable) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, pageable));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Delete owned comment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal) { // 3. Change to UserPrincipal

        commentService.deleteComment(commentId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}