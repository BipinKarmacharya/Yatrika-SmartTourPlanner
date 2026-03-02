package com.yatrika.interest.repository;

import com.yatrika.interest.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    Optional<Interest> findByCode(String code);
    List<Interest> findByActiveTrue();
}
