package com.yatrika.notification.repository;

import com.yatrika.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByUserId(Long userId);

    boolean existsByToken(String token);

    @Modifying
    @Transactional
    void deleteByToken(String token); // Useful for cleaning up expired tokens
}