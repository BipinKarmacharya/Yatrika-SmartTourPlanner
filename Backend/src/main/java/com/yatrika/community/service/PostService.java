package com.yatrika.community.service;

import com.yatrika.community.domain.*;
import com.yatrika.community.dto.request.CreatePostRequest;
import com.yatrika.community.dto.request.UpdatePostRequest;
import com.yatrika.community.dto.response.PostResponse;
import com.yatrika.community.mapper.PostMapper;
import com.yatrika.community.repository.PostLikeRepository;
import com.yatrika.community.repository.PostRepository;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.user.domain.User;
import com.yatrika.user.service.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CurrentUserService currentUserService;
    private final PostMapper postMapper;

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        User currentUser = currentUserService.getCurrentUserEntity();

        Post post = Post.builder()
                .user(currentUser)
                .title(request.getTitle())
                .content(request.getContent())
                .tripDurationDays(request.getTripDurationDays())
                .estimatedCost(request.getEstimatedCost())
                .coverImageUrl(request.getCoverImageUrl())
                .isPublic(request.getIsPublic())
                .build();

        // Add media items
        if (request.getMedia() != null) {
            request.getMedia().forEach(mediaRequest -> {
                PostMedia media = PostMedia.builder()
                        .post(post)
                        .mediaUrl(mediaRequest.getMediaUrl())
                        .mediaType(mediaRequest.getMediaType())
                        .caption(mediaRequest.getCaption())
                        .dayNumber(mediaRequest.getDayNumber())
                        .displayOrder(mediaRequest.getDisplayOrder())
                        .build();
                post.addMedia(media);
            });
        }

        // Add day descriptions
        if (request.getDays() != null) {
            request.getDays().forEach(dayRequest -> {
                PostDay day = PostDay.builder()
                        .post(post)
                        .dayNumber(dayRequest.getDayNumber())
                        .description(dayRequest.getDescription())
                        .activities(dayRequest.getActivities())
                        .accommodation(dayRequest.getAccommodation())
                        .food(dayRequest.getFood())
                        .transportation(dayRequest.getTransportation())
                        .build();
                post.addDay(day);
            });
        }

        Post savedPost = postRepository.save(post);
        log.info("Post created: {} by user {}", post.getTitle(), currentUser.getId());

        return postMapper.toResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User currentUser = currentUserService.getCurrentUserEntity();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("You can only update your own posts");
        }

        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getCoverImageUrl() != null) post.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getIsPublic() != null) post.setIsPublic(request.getIsPublic());

        Post updatedPost = postRepository.save(post);
        return enrichPostResponse(updatedPost);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User currentUser = currentUserService.getCurrentUserEntity();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("You can only delete your own posts");
        }

        postRepository.delete(post);
        log.info("Post deleted: {}", postId);
    }

    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Check if user can view the post
        User currentUser = currentUserService.getCurrentUserEntityOrNull();
        if (!post.getIsPublic()) {
            if (currentUser == null || !post.getUser().getId().equals(currentUser.getId())) {
                throw new AppException("This post is private");
            }
        }

        // Increment view count
        post.incrementViews();
        postRepository.save(post);

        return enrichPostResponse(post);
    }

    public Page<PostResponse> getMyPosts(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUserEntity();
        Page<Post> posts = postRepository.findByUserId(currentUser.getId(), pageable);
        return posts.map(this::enrichPostResponse);
    }

    public Page<PostResponse> getPublicPosts(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUserEntityOrNull();
        Page<Post> posts;

        if (currentUser != null) {
            // Exclude user's own posts from public feed
            posts = postRepository.findByIsPublicTrueAndUserIdNot(currentUser.getId(), pageable);
        } else {
            posts = postRepository.findByIsPublicTrue(pageable);
        }

        return posts.map(this::enrichPostResponse);
    }

    public Page<PostResponse> searchPosts(String search, Pageable pageable) {
        Page<Post> posts = postRepository.searchPublicPosts(search, pageable);
        return posts.map(this::enrichPostResponse);
    }

    public Page<PostResponse> getTrendingPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findTrendingPosts(pageable);
        return posts.map(this::enrichPostResponse);
    }

    @Transactional
    public void likePost(Long postId) {
        User currentUser = currentUserService.getCurrentUserEntity();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Check if already liked
        if (postLikeRepository.existsByPostIdAndUserId(postId, currentUser.getId())) {
            throw new AppException("You have already liked this post");
        }

        PostLike postLike = PostLike.builder()
                .post(post)
                .user(currentUser)
                .build();

        postLikeRepository.save(postLike);
        post.incrementLikes();
        postRepository.save(post);

        log.info("User {} liked post {}", currentUser.getId(), postId);
    }

    @Transactional
    public void unlikePost(Long postId) {
        User currentUser = currentUserService.getCurrentUserEntity();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        PostLike postLike = postLikeRepository.findByPostIdAndUserId(postId, currentUser.getId())
                .orElseThrow(() -> new AppException("You have not liked this post"));

        postLikeRepository.delete(postLike);
        post.decrementLikes();
        postRepository.save(post);

        log.info("User {} unliked post {}", currentUser.getId(), postId);
    }

    private PostResponse enrichPostResponse(Post post) {
        PostResponse response = postMapper.toResponse(post);

        // Check if current user has liked this post
        User currentUser = currentUserService.getCurrentUserEntityOrNull();
        if (currentUser != null) {
            boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
            response.setIsLikedByCurrentUser(isLiked);
        } else {
            response.setIsLikedByCurrentUser(false);
        }

        return response;
    }

    // Statistics methods
    public Long getUserPostCount(Long userId) {
        return postRepository.countByUserId(userId);
    }

    public Long getUserLikesCount(Long userId) {
        return postLikeRepository.countByUserId(userId);
    }
}