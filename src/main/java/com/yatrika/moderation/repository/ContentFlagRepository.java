package com.yatrika.moderation.repository;

import com.yatrika.moderation.domain.ContentFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContentFlagRepository extends JpaRepository<ContentFlag, Long> {

    long countByStatus(ContentFlag.FlagStatus status);

    Page<ContentFlag> findByStatus(ContentFlag.FlagStatus status, Pageable pageable);

    Page<ContentFlag> findByContentTypeAndContentId(ContentFlag.ContentType contentType, Long contentId, Pageable pageable);

    boolean existsByContentTypeAndContentIdAndReporterId(
            ContentFlag.ContentType contentType,
            Long contentId,
            Long reporterId);

    @Query("SELECT cf FROM ContentFlag cf WHERE " +
            "(:status IS NULL OR cf.status = :status) AND " +
            "(:contentType IS NULL OR cf.contentType = :contentType) AND " +
            "(:startDate IS NULL OR cf.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR cf.createdAt <= :endDate)")
    Page<ContentFlag> findWithFilters(
            @Param("status") ContentFlag.FlagStatus status,
            @Param("contentType") ContentFlag.ContentType contentType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}