// com.yatrika.moderation.service.ModerationService.java
package com.yatrika.moderation.service;

import com.yatrika.community.domain.Post;
import com.yatrika.community.repository.PostRepository;
import com.yatrika.moderation.domain.ContentFlag;
import com.yatrika.moderation.repository.ContentFlagRepository;
import com.yatrika.review.domain.Review;
import com.yatrika.review.repository.ReviewRepository;
import com.yatrika.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private final ContentFlagRepository contentFlagRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ContentFlag flagContent(ContentFlag.ContentType contentType, Long contentId,
                                   String reason, User reporter) {
        // Check if user already flagged this content
        if (contentFlagRepository.existsByContentTypeAndContentIdAndReporterId(
                contentType, contentId, reporter.getId())) {
            throw new RuntimeException("You have already flagged this content");
        }

        ContentFlag flag = ContentFlag.builder()
                .contentType(contentType)
                .contentId(contentId)
                .reporter(reporter)
                .reason(reason)
                .status(ContentFlag.FlagStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return contentFlagRepository.save(flag);
    }

    @Transactional
    public void removeContent(ContentFlag.ContentType contentType, Long contentId) {
        if (contentType == ContentFlag.ContentType.POST) {
            Post post = postRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            postRepository.delete(post);
        } else {
            Review review = reviewRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));
            reviewRepository.delete(review);
        }
    }
}