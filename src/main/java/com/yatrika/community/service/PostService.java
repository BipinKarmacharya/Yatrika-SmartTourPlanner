package com.yatrika.community.service;

import com.yatrika.community.domain.*;
import com.yatrika.community.dto.request.CreatePostRequest;
import com.yatrika.community.dto.request.UpdatePostRequest;
import com.yatrika.community.dto.response.PostDayResponse;
import com.yatrika.community.dto.response.PostMediaResponse;
import com.yatrika.community.dto.response.PostResponse;
import com.yatrika.community.mapper.PostMapper;
import com.yatrika.community.repository.PostLikeRepository;
import com.yatrika.community.repository.PostRepository;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.shared.service.FileStorageService;
import com.yatrika.user.domain.User;
import com.yatrika.user.service.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CurrentUserService currentUserService;
    private final PostMapper postMapper;
    private final FileStorageService fileStorageService;

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
                .destination(request.getDestination())
                .tags(request.getTags() != null ? request.getTags() : new HashSet<>())
                .build();

        if (request.getMedia() != null) {
            request.getMedia().forEach(mediaReq -> {
                PostMedia media = PostMedia.builder()
                        .mediaUrl(mediaReq.getMediaUrl())
                        .mediaType(mediaReq.getMediaType())
                        .caption(mediaReq.getCaption())
                        .dayNumber(mediaReq.getDayNumber())
                        .displayOrder(mediaReq.getDisplayOrder())
                        .build();
                post.addMedia(media);
            });
        }

        if (request.getDays() != null) {
            request.getDays().forEach(dayReq -> {
                PostDay day = PostDay.builder()
                        .dayNumber(dayReq.getDayNumber())
                        .description(dayReq.getDescription())
                        .activities(dayReq.getActivities())
                        .accommodation(dayReq.getAccommodation())
                        .food(dayReq.getFood())
                        .transportation(dayReq.getTransportation())
                        .build();
                post.addDay(day);
            });
        }

        Post savedPost = postRepository.save(post);
        log.info("Post created: {} by user {}", post.getTitle(), currentUser.getId());
        return enrichPostResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User currentUser = currentUserService.getCurrentUserEntity();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("You can only update your own posts");
        }

        // 1. Sync Cover Image
        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().equals(post.getCoverImageUrl())) {
            fileStorageService.deleteFile(post.getCoverImageUrl());
            post.setCoverImageUrl(request.getCoverImageUrl());
        }

        // 2. Sync Media (Delete removed files from storage)
        if (request.getMedia() != null) {
            List<String> newMediaUrls = request.getMedia().stream()
                    .map(m -> m.getMediaUrl())
                    .collect(Collectors.toList());

            List<PostMedia> toRemove = post.getMedia().stream()
                    .filter(existing -> !newMediaUrls.contains(existing.getMediaUrl()))
                    .toList();

            for (PostMedia media : toRemove) {
                fileStorageService.deleteFile(media.getMediaUrl());
                post.getMedia().remove(media);
                media.setPost(null);
            }

            // Note: Adding new media items usually happens via CreatePostRequest pattern,
            // but for simplicity, we focus on the removal/cleanup here.
        }

        // 3. Update basic fields
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getIsPublic() != null) post.setIsPublic(request.getIsPublic());
        if (request.getTripDurationDays() != null) post.setTripDurationDays(request.getTripDurationDays());
        if (request.getEstimatedCost() != null) post.setEstimatedCost(request.getEstimatedCost());

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

        // Collect all file URLs before deleting DB records
        List<String> urlsToDelete = new ArrayList<>();
        if (post.getCoverImageUrl() != null) urlsToDelete.add(post.getCoverImageUrl());
        post.getMedia().forEach(m -> urlsToDelete.add(m.getMediaUrl()));

        postRepository.delete(post);

        // Physical deletion after DB success
        urlsToDelete.forEach(fileStorageService::deleteFile);
        log.info("Post {} and its {} media files deleted", postId, urlsToDelete.size());
    }

    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User currentUser = currentUserService.getCurrentUserEntityOrNull();
        if (!post.getIsPublic()) {
            if (currentUser == null || !post.getUser().getId().equals(currentUser.getId())) {
                throw new AppException("This post is private");
            }
        }

        post.incrementViews();
        postRepository.save(post);
        return enrichPostResponse(post);
    }

    public Page<PostResponse> getMyPosts(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUserEntity();
        return postRepository.findByUserId(currentUser.getId(), pageable).map(this::enrichPostResponse);
    }

    public Page<PostResponse> getPublicPosts(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUserEntityOrNull();
        Page<Post> posts = (currentUser != null)
                ? postRepository.findByIsPublicTrueAndUserIdNot(currentUser.getId(), pageable)
                : postRepository.findByIsPublicTrue(pageable);
        return posts.map(this::enrichPostResponse);
    }

    public Page<PostResponse> searchPosts(String query, Pageable pageable) {
        // If the query is null or just whitespace, return a default list or empty page
        if (query == null || query.trim().isEmpty()) {
            return postRepository.findAll(pageable).map(this::convertToResponse);
        }

        // Otherwise, perform the filtered search
        return postRepository.searchByKeyword(query, pageable).map(this::convertToResponse);
    }

    public Page<PostResponse> getTrendingPosts(Pageable pageable) {
        return postRepository.findTrendingPosts(pageable).map(this::enrichPostResponse);
    }

    @Transactional
    public void likePost(Long postId) {
        User currentUser = currentUserService.getCurrentUserEntity();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (postLikeRepository.existsByPostIdAndUserId(postId, currentUser.getId())) {
            throw new AppException("You have already liked this post");
        }

        postLikeRepository.save(PostLike.builder().post(post).user(currentUser).build());
        post.incrementLikes();
        postRepository.save(post);
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
    }

    private PostResponse enrichPostResponse(Post post) {
        PostResponse response = postMapper.toResponse(post);
        User currentUser = currentUserService.getCurrentUserEntityOrNull();
        boolean isLiked = currentUser != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
        response.setIsLikedByCurrentUser(isLiked);
        return response;
    }

    public Long getUserPostCount(Long userId) {
        return postRepository.countByUserId(userId);
    }

    public Long getUserLikesCount(Long userId) {
        return postLikeRepository.countByUserId(userId);
    }

    private PostResponse convertToResponse(Post post) {
        // 1. Let the mapper handle the heavy lifting
        // MapStruct automatically converts Entity Set -> DTO List
        PostResponse response = postMapper.toResponse(post);

        // 2. Add the 'Like' status (if not handled in mapper)
        User currentUser = currentUserService.getCurrentUserEntityOrNull();
        boolean isLiked = currentUser != null &&
                postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
        response.setIsLikedByCurrentUser(isLiked);

        return response;
    }

//    private PostResponse convertToResponse(Post post) {
//        // Better yet: Use the mapper you already have!
//        PostResponse response = postMapper.toResponse(post);
//
//        // If you MUST do it manually, use .collect(Collectors.toList())
//        if (post.getMedia() != null) {
//            response.setMedia(post.getMedia().stream()
//                    .map(m -> new PostMediaResponse(
//                            m.getId(),
//                            m.getMediaUrl(),
//                            m.getMediaType(),
//                            m.getCaption(),
//                            m.getDayNumber(),
//                            m.getDisplayOrder()
//                    ))
//                    .collect(Collectors.toList())); // Ensures it's a List
//        }
//
//        if (post.getDays() != null) {
//            response.setDays(post.getDays().stream()
//                    .map(d -> new PostDayResponse(
//                            d.getId(),
//                            d.getDayNumber(),
//                            d.getDescription(),
//                            d.getActivities(),
//                            d.getAccommodation(),
//                            d.getFood(),
//                            d.getTransportation()
//                    ))
//                    .sorted((a, b) -> a.getDayNumber().compareTo(b.getDayNumber())) // Keep days in order!
//                    .collect(Collectors.toList()));
//        }
//
//        return response;
//    }
}