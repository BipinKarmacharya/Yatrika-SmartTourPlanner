package com.yatrika.interest.controller;

import com.yatrika.interest.dto.request.InterestRequest;
import com.yatrika.interest.dto.response.InterestResponse;
import com.yatrika.interest.mapper.InterestMapper;
import com.yatrika.interest.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;
    private final InterestMapper interestMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Get all Interests",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<InterestResponse> getAllActiveInterests() {
        return interestService.getAllActiveInterests()
                .stream()
                .map(interestMapper::toResponse)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Add New Interest (ADMIN ONLY))",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public InterestResponse createInterest(@RequestBody InterestRequest request) {
        return interestMapper.toResponse(interestService.createInterest(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update Interest (ADMIN ONLY))",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public InterestResponse updateInterest(@PathVariable Long id, @RequestBody InterestRequest request) {
        return interestMapper.toResponse(interestService.updateInterest(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete Interest (ADMIN ONLY))",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public void deleteInterest(@PathVariable Long id) {
        interestService.deleteInterest(id);
    }
}


