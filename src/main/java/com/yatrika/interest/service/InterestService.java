package com.yatrika.interest.service;

import com.yatrika.interest.domain.Interest;
import com.yatrika.interest.domain.UserInterest;
import com.yatrika.interest.repository.InterestRepository;
import com.yatrika.interest.repository.UserInterestRepository;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.exception.ResourceNotFoundException;
import com.yatrika.user.domain.User;
import com.yatrika.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InterestService {

    private final InterestRepository interestRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserRepository userRepository;

    public List<Interest> getAllActiveInterests() {
        return interestRepository.findByActiveTrue();
    }

    public void updateUserInterests(Long userId, List<String> interestCodes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Remove old interests
        userInterestRepository.deleteByUserId(userId);

        // Add new ones
        for (String code : interestCodes) {
            Interest interest = interestRepository.findByCode(code)
                    .orElseThrow(() -> new AppException("Invalid interest: " + code));

            userInterestRepository.save(
                    UserInterest.builder()
                            .user(user)
                            .interest(interest)
                            .build()
            );
        }
    }
}

