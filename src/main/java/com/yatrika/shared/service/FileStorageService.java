package com.yatrika.shared.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String folder);
    void deleteFile(String fileUrl);
    boolean isValidImage(MultipartFile file);
}