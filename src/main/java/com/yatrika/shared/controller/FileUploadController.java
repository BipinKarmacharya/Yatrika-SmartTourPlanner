package com.yatrika.shared.controller;

import com.yatrika.shared.dto.FileUploadResponse;
import com.yatrika.shared.dto.ProfileUploadResponse;
import com.yatrika.shared.exception.AppException;
import com.yatrika.shared.service.FileStorageService;
import com.yatrika.user.dto.response.UserResponse;
import com.yatrika.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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

    private final FileStorageService fileStorageService;
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
}


//
//@RestController
//@RequestMapping("/api/uploads")
//@RequiredArgsConstructor
//@Slf4j
//@Tag(name = "File Upload", description = "File upload APIs")
//public class FileUploadController {
//
//    private final FileStorageService fileStorageService;
//    private final CurrentUserService currentUserService;
//
//    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('USER')")
//    @Operation(
//            summary = "Upload profile picture",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<ProfileUploadResponse> uploadProfilePicture(
//            @RequestParam("file") MultipartFile file) {
//
//        if (!fileStorageService.isValidImage(file)) {
//            throw new AppException("Only image files are allowed for profile pictures");
//        }
//
//        // Upload file
//        String fileUrl = fileStorageService.storeFile(file, "profiles");
//
//        // Update user's profile image in database
//        UserResponse updatedUser = currentUserService.updateProfileImage(fileUrl);
//
//        ProfileUploadResponse response = new ProfileUploadResponse(
//                file.getOriginalFilename(),
//                fileUrl,
//                file.getContentType(),
//                file.getSize(),
//                updatedUser
//        );
//
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping(value = "/destination", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(
//            summary = "Upload destination image (Admin only)",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<FileUploadResponse> uploadDestinationImage(
//            @RequestParam("file") MultipartFile file) {
//
//        if (!fileStorageService.isValidImage(file)) {
//            throw new AppException("Only image files are allowed for destination images");
//        }
//
//        String fileUrl = fileStorageService.storeFile(file, "destinations");
//
//        FileUploadResponse response = new FileUploadResponse(
//                file.getOriginalFilename(),
//                fileUrl,
//                file.getContentType(),
//                file.getSize()
//        );
//
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping(value = "/post/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('USER')")
//    @Operation(
//            summary = "Upload post media (image/video)",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<List<FileUploadResponse>> uploadPostMedia(
//            @RequestParam("files") MultipartFile[] files) {
//
//        List<FileUploadResponse> responses = new ArrayList<>();
//
//        for (MultipartFile file : files) {
//            if (!fileStorageService.isValidImage(file) && !fileStorageService.isValidVideo(file)) {
//                throw new AppException("Only image and video files are allowed");
//            }
//
//            String fileUrl = fileStorageService.storeFile(file, "posts");
//
//            FileUploadResponse response = new FileUploadResponse(
//                    file.getOriginalFilename(),
//                    fileUrl,
//                    file.getContentType(),
//                    file.getSize()
//            );
//
//            responses.add(response);
//        }
//
//        return ResponseEntity.ok(responses);
//    }
//
//    // 1. Bulk Upload for Posts (User & Admin)
//    @PostMapping(value = "/post/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
//    @Operation(summary = "Upload multiple post media", security = @SecurityRequirement(name = "bearerAuth"))
//    public ResponseEntity<List<FileUploadResponse>> uploadPostBulk(
//            @RequestPart("files") MultipartFile[] files) {
//        return processBulkUpload(files, "posts");
//    }
//
//    // 2. Bulk Upload for Destinations (Admin Only)
//    @PostMapping(value = "/destination/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Upload multiple destination images", security = @SecurityRequirement(name = "bearerAuth"))
//    public ResponseEntity<List<FileUploadResponse>> uploadDestinationBulk(
//            @RequestPart("files") MultipartFile[] files) {
//        return processBulkUpload(files, "destinations");
//    }
//
//    // 3. Private Helper Method to avoid code duplication
//    private ResponseEntity<List<FileUploadResponse>> processBulkUpload(MultipartFile[] files, String folder) {
//        List<FileUploadResponse> responses = new ArrayList<>();
//        for (MultipartFile file : files) {
//            if (!fileStorageService.isValidImage(file) &&
//                    (!folder.equals("posts") || !fileStorageService.isValidVideo(file))) {
//                throw new AppException("Invalid file format for " + folder);
//            }
//
//            String fileUrl = fileStorageService.storeFile(file, folder);
//            responses.add(new FileUploadResponse(
//                    file.getOriginalFilename(),
//                    fileUrl,
//                    file.getContentType(),
//                    file.getSize()
//            ));
//        }
//        return ResponseEntity.ok(responses);
//    }
//
//    @DeleteMapping
//    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
//    @Operation(
//            summary = "Delete uploaded file",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    public ResponseEntity<Void> deleteFile(@RequestParam String fileUrl) {
//        // 🛡️ Security Check: Prevent non-admins from deleting destination files
//        if (fileUrl.contains("/destinations/") && !currentUserService.isAdmin()) {
//            log.warn("Unauthorized delete attempt on destination file by user");
//            throw new AppException("Access Denied: Only Admins can delete destination images.");
//        }
//
//        fileStorageService.deleteFile(fileUrl);
//        return ResponseEntity.noContent().build();
//    }
//}