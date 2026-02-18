package com.yatrika.interest.controller;

import com.yatrika.interest.dto.response.InterestResponse;
import com.yatrika.interest.mapper.InterestMapper;
import com.yatrika.interest.service.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;
    private final InterestMapper interestMapper;

    @GetMapping
    public List<InterestResponse> getAllActiveInterests() {
        return interestService.getAllActiveInterests()
                .stream()
                .map(interestMapper::toResponse)
                .toList();
    }
}


