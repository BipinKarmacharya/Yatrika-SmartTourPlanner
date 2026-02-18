package com.yatrika.shared.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.yatrika.shared.service.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageServiceImpl implements CloudinaryStorageService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folderName) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "yatrika/" + folderName));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image to Cloudinary");
        }
    }

    @Override
    public List<String> uploadMultipleFiles(List<MultipartFile> files, String folderName) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                urls.add(uploadFile(file, folderName));
            }
        }
        return urls;
    }

    @Override
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Cloudinary delete failed: {}", e.getMessage());
        }
    }
}