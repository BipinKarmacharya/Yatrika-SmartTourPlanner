package com.yatrika.community.service;

import com.yatrika.community.dto.request.CreatePostRequest;
import com.yatrika.community.dto.request.UpdatePostRequest;
import com.yatrika.community.dto.response.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {

    // Create a post with an optional image
    public PostResponse createPost(CreatePostRequest request, List<MultipartFile> imageFiles) ;

    // Update post text and/or replace the cover image
    PostResponse updatePost(Long postId, UpdatePostRequest request, List<MultipartFile> imageFile);

    // Delete post logic
    void deletePost(Long postId);

    // Fetch single post details (increments views)
    PostResponse getPostById(Long postId);

    // --- Ownership & Security ---
    // This is required for @PreAuthorize("@postServiceImpl.isPostOwner(#id)") in Controller
    boolean isPostOwner(Long postId);

    // --- Feed & Discovery ---
    Page<PostResponse> getMyPosts(Pageable pageable);

    Page<PostResponse> getPublicPosts(Pageable pageable);

    Page<PostResponse> searchPosts(String query, Pageable pageable);

    Page<PostResponse> getTrendingPosts(Pageable pageable);

    // --- Social Interactions ---
    PostResponse toggleLikePost(Long postId);

    // --- Statistics ---
    Long getUserPostCount(Long userId);

    Long getUserLikesCount(Long userId);
}