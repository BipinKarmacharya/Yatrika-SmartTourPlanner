package com.yatrika.itinerary.controller;

import com.yatrika.itinerary.dto.request.ItineraryItemRequest;
import com.yatrika.itinerary.dto.request.ItineraryRequest;
import com.yatrika.itinerary.dto.response.ItineraryResponse;
import com.yatrika.itinerary.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/itineraries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Itinerary Management", description = "Endpoints for creating expert curated templates")
public class AdminItineraryController {

    private final ItineraryService itineraryService;

    // ================= TEMPLATE =================

    @PostMapping
    @Operation(
            summary = "Create an admin itinerary template (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> createTemplate(@RequestBody ItineraryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.createAdminTemplate(request));
    }

    @PostMapping(
            value = "/with-images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Create admin template with images (one-step)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> createTemplateWithImages(
            @RequestPart("data") ItineraryRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.createAdminTemplate(request, files));
    }


    @PutMapping("/{id}")
    @Operation(
            summary = "Update admin template header",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> updateTemplate(
            @PathVariable Long id,
            @RequestBody ItineraryRequest request) {

        return ResponseEntity.ok(
                itineraryService.updateAdminTemplate(id, request)
        );
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(
            summary = "Delete admin itinerary image",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        return ResponseEntity.ok(
                itineraryService.removeAdminTemplateImage(id, imageId)
        );
    }

    @PutMapping("/{id}/images/{imageId}/cover")
    @Operation(
            summary = "Set cover image for admin itinerary",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> setCoverImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        return ResponseEntity.ok(
                itineraryService.setAdminTemplateCoverImage(id, imageId)
        );
    }


    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete admin template",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {

        itineraryService.removeAdminTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/{id}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ItineraryResponse uploadAdminTemplateImages(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return itineraryService.uploadAdminTemplateImages(id, files);
    }


    // ================= TEMPLATE ITEMS =================

    @PostMapping("/{id}/items")
    @Operation(
            summary = "Create an admin itinerary items template (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> addItem(@PathVariable Long id, @RequestBody ItineraryItemRequest request) {
        return ResponseEntity.ok(itineraryService.addItemToTemplate(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(
            summary = "Update admin template item",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ItineraryResponse> updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody ItineraryItemRequest request) {

        return ResponseEntity.ok(
                itineraryService.updateTemplateItem(id, itemId, request)
        );
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(
            summary = "Delete admin template item",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @PathVariable Long itemId) {

        itineraryService.removeItemFromTemplate(id, itemId);
        return ResponseEntity.noContent().build();
    }
}