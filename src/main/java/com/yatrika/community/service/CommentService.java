package com.yatrika.community.service;

import com.yatrika.community.domain.Comment;
import com.yatrika.community.domain.Post;
import com.yatrika.community.dto.request.CommentRequestDTO;
import com.yatrika.community.dto.response.CommentResponseDTO;
import com.yatrika.community.repository.CommentRepository;
import com.yatrika.community.repository.PostRepository;
import com.yatrika.user.domain.User;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponseDTO addComment(Long postId, Long userId, CommentRequestDTO request) { // 2. Change User to Long userId
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // 3. Fetch the actual managed Entity from DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(user) // Now this is the real DB entity
                .post(post)
                .build();

        post.addComment(comment);

        Comment savedComment = commentRepository.save(comment);
        return mapToResponseDTO(savedComment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponseDTO> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        // This method is already fine since it used userId!
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }

        Post post = comment.getPost();
        post.removeComment(comment);
        commentRepository.delete(comment);
    }

    private CommentResponseDTO mapToResponseDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .userProfileImageUrl(comment.getUser().getProfileImageUrl())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}