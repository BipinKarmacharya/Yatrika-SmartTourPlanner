package com.yatrika.community.repository;

import com.yatrika.community.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Finds all comments for a specific post with pagination
    Page<Comment> findByPostId(Long postId, Pageable pageable);
}