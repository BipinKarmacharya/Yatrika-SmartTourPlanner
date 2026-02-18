package com.yatrika.shared.controller;

import com.yatrika.shared.dto.FileUploadResponse;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.service.impl.LocalStorageServiceImpl;
import com.yatrika.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Tag(name = "File Upload")
public class FileUploadController {

    private final LocalStorageServiceImpl fileStorageService;
    private final CurrentUserService currentUserService;

    // Standardize the response for a single file
    private FileUploadResponse toResponse(MultipartFile file, String url) {
        return new FileUploadResponse(file.getOriginalFilename(), url, file.getContentType(), file.getSize());
    }

    @PostMapping("/destination")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FileUploadResponse> uploadDestinationImage(@RequestParam("file") MultipartFile file) {
        if (!fileStorageService.isValidImage(file)) throw new AppException("Images only");
        String url = fileStorageService.storeFile(file, "destinations");
        return ResponseEntity.ok(toResponse(file, url));
    }

    // Change this from "/post/bulk" to "/post/media"
    @PostMapping("/post/media")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<FileUploadResponse>> uploadPostMedia(@RequestParam("files") MultipartFile[] files) {
        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!fileStorageService.isValidImage(file)) {
                continue; // Or throw exception
            }
            String url = fileStorageService.storeFile(file, "posts");
            responses.add(toResponse(file, url));
        }
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Delete uploaded file",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteFile(@RequestParam String fileUrl) {
        // 🛡️ Security Check: Prevent non-admins from deleting destination files
        if (fileUrl.contains("/destinations/") && !currentUserService.isAdmin()) {
            throw new AppException("Access Denied: Only Admins can delete destination images.");
        }
        fileStorageService.deleteFile(fileUrl);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/itinerary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FileUploadResponse> uploadItineraryImage(@RequestParam("file") MultipartFile file) {
        if (!fileStorageService.isValidImage(file)) throw new AppException("Images only");
        // This will now automatically go to Cloudinary
        String url = fileStorageService.storeFile(file, "itineraries");
        return ResponseEntity.ok(toResponse(file, url));
    }
}
