package com.yatrika.shared.service;

import com.yatrika.shared.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// ... existing imports ...

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) throw new AppException("File is empty");

        // Use the properties for max size if you have them injected,
        // otherwise keep your 10MB check
        if (file.getSize() > 10 * 1024 * 1024) throw new AppException("File exceeds 10MB");

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID() + getFileExtension(originalFilename);

        Path uploadPath = Paths.get(uploadDir, subDirectory);
        try {
            Files.createDirectories(uploadPath);
            Path targetLocation = uploadPath.resolve(uniqueFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            // Return consistent URL format
            return "/uploads/" + subDirectory + "/" + uniqueFilename;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new AppException("Could not store file. Please try again.");
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            // Convert URL (/uploads/posts/xyz.jpg) to Path (./uploads/posts/xyz.jpg)
            // Assuming your uploadDir is "./uploads" and URL starts with "/uploads"
            String pathInFileSystem = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(pathInFileSystem);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Physical file deleted: {}", fileUrl);
            } else {
                log.warn("File not found for deletion: {}", fileUrl);
            }
        } catch (Exception e) {
            log.error("Error deleting physical file: {}", fileUrl, e);
            // We don't throw exception here so DB transaction can continue
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex > 0) ? filename.substring(lastDotIndex).toLowerCase() : "";
    }

    public boolean isValidImage(MultipartFile file) {
        String type = file.getContentType();
        return type != null && type.startsWith("image/");
    }
}
//@Service
//@Slf4j
//public class FileStorageService {
//
//    @Value("${app.upload.dir:./uploads}")
//    private String uploadDir;
//
//    public String storeFile(MultipartFile file, String subDirectory) {
//        // Validate file
//        if (file.isEmpty()) {
//            throw new AppException("File is empty");
//        }
//
//        // Check file size (max 10MB)
//        if (file.getSize() > 10 * 1024 * 1024) {
//            throw new AppException("File size exceeds 10MB limit");
//        }
//
//        // Get original filename
//        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
//
//        // Validate filename
//        if (originalFilename.contains("..")) {
//            throw new AppException("Invalid file path: " + originalFilename);
//        }
//
//        // Generate unique filename
//        String fileExtension = getFileExtension(originalFilename);
//        String uniqueFilename = UUID.randomUUID() + fileExtension;
//
//        // Create directory if not exists
//        Path uploadPath = Paths.get(uploadDir, subDirectory);
//        try {
//            Files.createDirectories(uploadPath);
//        } catch (IOException e) {
//            throw new AppException("Could not create upload directory");
//        }
//
//        // Save file
//        Path targetLocation = uploadPath.resolve(uniqueFilename);
//        try (InputStream inputStream = file.getInputStream()) {
//            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
//        } catch (IOException e) {
//            throw new AppException("Could not save file: " + e.getMessage());
//        }
//
//        // Return relative path
//        String fileUrl = "/uploads/" + subDirectory + "/" + uniqueFilename;
//        log.info("File uploaded: {}", fileUrl);
//        return fileUrl;
//    }
//
//    public void deleteFile(String fileUrl) {
//        if (fileUrl == null || fileUrl.isEmpty()) {
//            return;
//        }
//
//        try {
//            // Remove leading slash if present
//            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
//            Path filePath = Paths.get(relativePath);
//
//            if (Files.exists(filePath)) {
//                Files.delete(filePath);
//                log.info("File deleted: {}", fileUrl);
//            }
//        } catch (IOException e) {
//            log.error("Could not delete file: {}", fileUrl, e);
//        }
//    }
//
//    private String getFileExtension(String filename) {
//        int lastDotIndex = filename.lastIndexOf('.');
//        if (lastDotIndex > 0) {
//            return filename.substring(lastDotIndex).toLowerCase();
//        }
//        return "";
//    }
//
//    public boolean isValidImage(MultipartFile file) {
//        String contentType = file.getContentType();
//        return contentType != null && contentType.startsWith("image/");
//    }
//
//    public boolean isValidVideo(MultipartFile file) {
//        String contentType = file.getContentType();
//        return contentType != null && contentType.startsWith("video/");
//    }
//}