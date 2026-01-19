package com.yatrika.user.repository;

import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    long countByIsActive(Boolean isActive);

    // Admin methods
    Page<User> findByIsActive(boolean isActive, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:active IS NULL OR u.isActive = :active)")

    Page<User> findUsersWithFilters(
            @Param("search") String search,
            @Param("role") UserRole role,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT FUNCTION('DATE', u.createdAt) as date, COUNT(u) as count FROM User u " +
            "WHERE u.createdAt >= :startDate GROUP BY FUNCTION('DATE', u.createdAt) ORDER BY date")

    List<Object[]> countNewUsersPerDay(@Param("startDate") LocalDate startDate);
}
