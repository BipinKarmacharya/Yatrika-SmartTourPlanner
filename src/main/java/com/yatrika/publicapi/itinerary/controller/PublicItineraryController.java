package com.yatrika.publicapi.itinerary.controller;

import com.yatrika.admin.itinerary.dto.response.AdminItineraryResponse;
import com.yatrika.admin.itinerary.mapper.AdminItineraryMapper;
import com.yatrika.admin.itinerary.repository.AdminItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/itineraries")
@RequiredArgsConstructor
public class PublicItineraryController {

    private final AdminItineraryRepository itineraryRepository;
    private final AdminItineraryMapper mapper;

    // ================= GET ALL PUBLISHED =================
    @GetMapping
    public List<AdminItineraryResponse> getAllPublished() {
        return itineraryRepository.findByActiveTrue()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // ================= GET BY ITINERARY ID =================
    @GetMapping("/{id}")
    public AdminItineraryResponse getById(@PathVariable Long id) {
        return itineraryRepository.findById(id)
                .filter(it -> Boolean.TRUE.equals(it.getActive()))
                .map(mapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));
    }

    // ================= 🔥 GET BY DESTINATION =================
    @GetMapping("/destination/{destinationId}")
    public List<AdminItineraryResponse> getByDestination(
            @PathVariable Long destinationId
    ) {
        return itineraryRepository.findActiveByDestinationId(destinationId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
