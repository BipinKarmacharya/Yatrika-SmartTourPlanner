package com.yatrika.community.service.impl;

import com.yatrika.community.domain.*;
import com.yatrika.community.dto.request.CreatePostRequest;
import com.yatrika.community.dto.request.UpdatePostRequest;
import com.yatrika.community.dto.response.PostResponse;
import com.yatrika.community.mapper.PostMapper;
import com.yatrika.community.repository.PostLikeRepository;
import com.yatrika.community.repository.PostRepository;
import com.yatrika.community.service.PostService;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.shared.service.CloudinaryStorageService;
import com.yatrika.user.domain.User;
import com.yatrika.user.repository.FollowRepository;
import com.yatrika.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CurrentUserService currentUserService;
    private final PostMapper postMapper;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final FollowRepository followRepository;

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, List<MultipartFile> imageFiles) {
        User currentUser = currentUserService.getCurrentUserEntity();

        Post post = Post.builder()
                .user(currentUser)
                .title(request.getTitle())
                .content(request.getContent())
                .tripDurationDays(request.getTripDurationDays())
                .estimatedCost(request.getEstimatedCost())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .destination(request.getDestination())
                .tags(request.getTags() != null ? request.getTags() : new HashSet<>())
                .build();

        // 1. Handle Multiple Image Uploads
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile file = imageFiles.get(i);
                String url = cloudinaryStorageService.uploadFile(file, "community_posts");

                // Set the first image as the cover image
                if (i == 0) {
                    post.setCoverImageUrl(url);
                }

                // Add as media entry for the gallery
                post.addMedia(PostMedia.builder()
                        .mediaUrl(url)
                        .mediaType("IMAGE")
                        .displayOrder(i)
                        .build());
            }
        }

        mapRequestToEntity(request, post);
        Post savedPost = postRepository.save(post);
        return enrichPostResponse(savedPost);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request, List<MultipartFile> imageFiles) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        validateOwnership(post);

        // 2. Add new images if provided
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile file : imageFiles) {
                String url = cloudinaryStorageService.uploadFile(file, "community_posts");
                post.addMedia(PostMedia.builder()
                        .mediaUrl(url)
                        .mediaType("IMAGE")
                        .build());
            }
        }

        updateBasicFields(post, request);
        return enrichPostResponse(postRepository.save(post));
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        validateOwnership(post);
        postRepository.delete(post);
        log.info("Post deleted: ID {}", postId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPostOwner(Long postId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return postRepository.findById(postId)
                .map(post -> post.getUser().getId().equals(currentUserId))
                .orElse(false);
    }

    @Override
    @Transactional
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getIsPublic()) {
            validateOwnership(post);
        }

        post.incrementViews();
        return enrichPostResponse(postRepository.save(post));
    }

    @Override
    public Page<PostResponse> getMyPosts(Pageable pageable) {
        return postRepository.findByUserId(currentUserService.getCurrentUserId(), pageable)
                .map(this::enrichPostResponse);
    }

    @Override
    public Page<PostResponse> getPublicPosts(Pageable pageable) {
        return postRepository.findByIsPublicTrue(pageable)
                .map(this::enrichPostResponse);
    }

    @Override
    public Page<PostResponse> searchPosts(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return getPublicPosts(pageable);
        }
        return postRepository.searchByKeyword(query, pageable)
                .map(this::enrichPostResponse);
    }

    @Override
    public Page<PostResponse> getTrendingPosts(Pageable pageable) {
        return postRepository.findTrendingPosts(pageable)
                .map(this::enrichPostResponse);
    }

    @Override
    @Transactional
    public PostResponse toggleLikePost(Long postId) {
        User currentUser = currentUserService.getCurrentUserEntity();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, currentUser.getId());

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.decrementLikes();
        } else {
            postLikeRepository.save(PostLike.builder().post(post).user(currentUser).build());
            post.incrementLikes();
        }

        return enrichPostResponse(postRepository.save(post));
    }

    @Override
    public Long getUserPostCount(Long userId) {
        return postRepository.countByUserId(userId);
    }

    @Override
    public Long getUserLikesCount(Long userId) {
        return postLikeRepository.countByUserId(userId);
    }

    // --- Private Helper Methods ---

    private void validateOwnership(Post post) {
        User currentUser = currentUserService.getCurrentUserEntity();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("Access denied: You do not own this post");
        }
    }

    private void updateBasicFields(Post post, UpdatePostRequest request) {
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getIsPublic() != null) post.setIsPublic(request.getIsPublic());
        if (request.getDestination() != null) post.setDestination(request.getDestination());
        if (request.getTripDurationDays() != null) post.setTripDurationDays(request.getTripDurationDays());
        if (request.getEstimatedCost() != null) post.setEstimatedCost(request.getEstimatedCost());
    }

    private PostResponse enrichPostResponse(Post post) {
        PostResponse response = postMapper.toResponse(post);
        User currentUser = currentUserService.getCurrentUserEntityOrNull();

        if (currentUser != null) {
            response.setIsLikedByCurrentUser(postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId()));
            if (response.getUser() != null) {
                response.getUser().setIsFollowing(followRepository.existsByFollowerIdAndFollowingId(
                        currentUser.getId(), post.getUser().getId()));
            }
        }
        return response;
    }

    private void mapRequestToEntity(CreatePostRequest request, Post post) {
        if (request.getMedia() != null) {
            request.getMedia().forEach(m -> post.addMedia(PostMedia.builder()
                    .mediaUrl(m.getMediaUrl()).mediaType(m.getMediaType()).caption(m.getCaption()).build()));
        }
        if (request.getDays() != null) {
            request.getDays().forEach(d -> post.addDay(PostDay.builder()
                    .dayNumber(d.getDayNumber()).description(d.getDescription())
                    .accommodation(d.getAccommodation()).food(d.getFood()).build()));
        }
    }
}

//package com.yatrika.community.service.impl;
//
//import com.yatrika.community.domain.*;
//import com.yatrika.community.dto.request.CreatePostRequest;
//import com.yatrika.community.dto.request.UpdatePostRequest;
//import com.yatrika.community.dto.response.PostResponse;
//import com.yatrika.community.mapper.PostMapper;
//import com.yatrika.community.repository.PostLikeRepository;
//import com.yatrika.community.repository.PostRepository;
//import com.yatrika.shared.exception.AppException;
//import com.yatrika.shared.exception.ResourceNotFoundException;
//import com.yatrika.shared.service.impl.LocalStorageServiceImpl;
//import com.yatrika.user.domain.User;
//import com.yatrika.user.repository.FollowRepository;
//import com.yatrika.user.service.CurrentUserService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PostServiceImpl {
//
//    private final PostRepository postRepository;
//    private final PostLikeRepository postLikeRepository;
//    private final CurrentUserService currentUserService;
//    private final PostMapper postMapper;
//    private final LocalStorageServiceImpl fileStorageService;
//    private final FollowRepository followRepository;
//
//    @Transactional
//    public PostResponse createPost(CreatePostRequest request) {
//        User currentUser = currentUserService.getCurrentUserEntity();
//
//        Post post = Post.builder()
//                .user(currentUser)
//                .title(request.getTitle())
//                .content(request.getContent())
//                .tripDurationDays(request.getTripDurationDays())
//                .estimatedCost(request.getEstimatedCost())
//                .coverImageUrl(request.getCoverImageUrl())
//                .isPublic(request.getIsPublic())
//                .destination(request.getDestination())
//                .tags(request.getTags() != null ? request.getTags() : new HashSet<>())
//                .build();
//
//        if (request.getMedia() != null) {
//            request.getMedia().forEach(mediaReq -> {
//                PostMedia media = PostMedia.builder()
//                        .mediaUrl(mediaReq.getMediaUrl())
//                        .mediaType(mediaReq.getMediaType())
//                        .caption(mediaReq.getCaption())
//                        .dayNumber(mediaReq.getDayNumber())
//                        .displayOrder(mediaReq.getDisplayOrder())
//                        .build();
//                post.addMedia(media);
//            });
//        }
//
//        if (request.getDays() != null) {
//            request.getDays().forEach(dayReq -> {
//                PostDay day = PostDay.builder()
//                        .dayNumber(dayReq.getDayNumber())
//                        .description(dayReq.getDescription())
//                        .activities(dayReq.getActivities())
//                        .accommodation(dayReq.getAccommodation())
//                        .food(dayReq.getFood())
//                        .transportation(dayReq.getTransportation())
//                        .build();
//                post.addDay(day);
//            });
//        }
//
//        Post savedPost = postRepository.save(post);
//        log.info("Post created: {} by user {}", post.getTitle(), currentUser.getId());
//        return enrichPostResponse(savedPost);
//    }
//
//    @Transactional
//    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
//
//        User currentUser = currentUserService.getCurrentUserEntity();
//        if (!post.getUser().getId().equals(currentUser.getId())) {
//            throw new AppException("You can only update your own posts");
//        }
//
//        // 1. Sync Cover Image
//        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().equals(post.getCoverImageUrl())) {
//            fileStorageService.deleteFile(post.getCoverImageUrl());
//            post.setCoverImageUrl(request.getCoverImageUrl());
//        }
//
//        // 2. Sync Media (Delete removed files from storage)
//        if (request.getMedia() != null) {
//            List<String> newMediaUrls = request.getMedia().stream()
//                    .map(m -> m.getMediaUrl())
//                    .collect(Collectors.toList());
//
//            List<PostMedia> toRemove = post.getMedia().stream()
//                    .filter(existing -> !newMediaUrls.contains(existing.getMediaUrl()))
//                    .toList();
//
//            for (PostMedia media : toRemove) {
//                fileStorageService.deleteFile(media.getMediaUrl());
//                post.getMedia().remove(media);
//                media.setPost(null);
//            }
//
//            // Note: Adding new media items usually happens via CreatePostRequest pattern,
//            // but for simplicity, we focus on the removal/cleanup here.
//        }
//
//        // 3. Update basic fields
//        if (request.getTitle() != null) post.setTitle(request.getTitle());
//        if (request.getContent() != null) post.setContent(request.getContent());
//        if (request.getIsPublic() != null) post.setIsPublic(request.getIsPublic());
//        if (request.getTripDurationDays() != null) post.setTripDurationDays(request.getTripDurationDays());
//        if (request.getEstimatedCost() != null) post.setEstimatedCost(request.getEstimatedCost());
//
//        Post updatedPost = postRepository.save(post);
//        return enrichPostResponse(updatedPost);
//    }
//
//    @Transactional
//    public void deletePost(Long postId) {
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
//
//        User currentUser = currentUserService.getCurrentUserEntity();
//        if (!post.getUser().getId().equals(currentUser.getId())) {
//            throw new AppException("You can only delete your own posts");
//        }
//
//        // Collect all file URLs before deleting DB records
//        List<String> urlsToDelete = new ArrayList<>();
//        if (post.getCoverImageUrl() != null) urlsToDelete.add(post.getCoverImageUrl());
//        post.getMedia().forEach(m -> urlsToDelete.add(m.getMediaUrl()));
//
//        postRepository.delete(post);
//
//        // Physical deletion after DB success
//        urlsToDelete.forEach(fileStorageService::deleteFile);
//        log.info("Post {} and its {} media files deleted", postId, urlsToDelete.size());
//    }
//
//    public PostResponse getPostById(Long postId) {
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
//
//        User currentUser = currentUserService.getCurrentUserEntityOrNull();
//        if (!post.getIsPublic()) {
//            if (currentUser == null || !post.getUser().getId().equals(currentUser.getId())) {
//                throw new AppException("This post is private");
//            }
//        }
//
//        post.incrementViews();
//        postRepository.save(post);
//        return enrichPostResponse(post);
//    }
//
//    public Page<PostResponse> getMyPosts(Pageable pageable) {
//        User currentUser = currentUserService.getCurrentUserEntity();
//        return postRepository.findByUserId(currentUser.getId(), pageable).map(this::enrichPostResponse);
//    }
//
//    public Page<PostResponse> getPublicPosts(Pageable pageable) {
//        Page<Post> posts = postRepository.findByIsPublicTrue(pageable);
//
//        return posts.map(this::enrichPostResponse);
//    }
//
//    public Page<PostResponse> searchPosts(String query, Pageable pageable) {
//        // If the query is null or just whitespace, return a default list or empty page
//        if (query == null || query.trim().isEmpty()) {
//            return postRepository.findAll(pageable).map(this::convertToResponse);
//        }
//
//        // Otherwise, perform the filtered search
//        return postRepository.searchByKeyword(query, pageable).map(this::convertToResponse);
//    }
//
//    public Page<PostResponse> getTrendingPosts(Pageable pageable) {
//        return postRepository.findTrendingPosts(pageable).map(this::enrichPostResponse);
//    }
//
//    @Transactional
//    public void likePost(Long postId) {
//        User currentUser = currentUserService.getCurrentUserEntity();
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
//
//        if (postLikeRepository.existsByPostIdAndUserId(postId, currentUser.getId())) {
//            throw new AppException("You have already liked this post");
//        }
//
//        postLikeRepository.save(PostLike.builder().post(post).user(currentUser).build());
//        post.incrementLikes();
//        postRepository.save(post);
//    }
//
//    @Transactional
//    public void unlikePost(Long postId) {
//        User currentUser = currentUserService.getCurrentUserEntity();
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
//
//        PostLike postLike = postLikeRepository.findByPostIdAndUserId(postId, currentUser.getId())
//                .orElseThrow(() -> new AppException("You have not liked this post"));
//
//        postLikeRepository.delete(postLike);
//        post.decrementLikes();
//        postRepository.save(post);
//    }
//
//    public Long getUserPostCount(Long userId) {
//        return postRepository.countByUserId(userId);
//    }
//
//    public Long getUserLikesCount(Long userId) {
//        return postLikeRepository.countByUserId(userId);
//    }
//
//    private PostResponse convertToResponse(Post post) {
//        // 1. Let the mapper handle the heavy lifting
//        // MapStruct automatically converts Entity Set -> DTO List
//        PostResponse response = postMapper.toResponse(post);
//
//        // 2. Add the 'Like' status (if not handled in mapper)
//        User currentUser = currentUserService.getCurrentUserEntityOrNull();
//        boolean isLiked = currentUser != null &&
//                postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
//        response.setIsLikedByCurrentUser(isLiked);
//
//        return response;
//    }
//
//    private PostResponse enrichPostResponse(Post post) {
//        PostResponse response = postMapper.toResponse(post);
//        User currentUser = currentUserService.getCurrentUserEntityOrNull();
//
//        if (currentUser != null) {
//            // 1. Check Like Status
//            boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
//            response.setIsLikedByCurrentUser(isLiked);
//
//            // 2. Check Follow Status for the Author
//            if (response.getUser() != null) {
//                boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(
//                        currentUser.getId(),
//                        post.getUser().getId()
//                );
//                response.getUser().setIsFollowing(isFollowing);
//            }
//        }
//        return response;
//    }
//}