package com.yatrika.shared.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CloudinaryStorageService {
    // For single images (Profile pics, Post image)
    String uploadFile(MultipartFile file, String folderName);

    // For multiple images (Itinerary gallery)
    List<String> uploadMultipleFiles(List<MultipartFile> files, String folderName);

    void deleteFile(String publicId);
}