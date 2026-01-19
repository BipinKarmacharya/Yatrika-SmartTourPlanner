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

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String subDirectory) {
        // Validate file
        if (file.isEmpty()) {
            throw new AppException("File is empty");
        }

        // Check file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new AppException("File size exceeds 10MB limit");
        }

        // Get original filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // Validate filename
        if (originalFilename.contains("..")) {
            throw new AppException("Invalid file path: " + originalFilename);
        }

        // Generate unique filename
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + fileExtension;

        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDir, subDirectory);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new AppException("Could not create upload directory");
        }

        // Save file
        Path targetLocation = uploadPath.resolve(uniqueFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException("Could not save file: " + e.getMessage());
        }

        // Return relative path
        String fileUrl = "/uploads/" + subDirectory + "/" + uniqueFilename;
        log.info("File uploaded: {}", fileUrl);
        return fileUrl;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // Remove leading slash if present
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted: {}", fileUrl);
            }
        } catch (IOException e) {
            log.error("Could not delete file: {}", fileUrl, e);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }

    public boolean isValidImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isValidVideo(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }
}