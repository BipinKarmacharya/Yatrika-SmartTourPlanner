package com.yatrika.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    private String uploadDir = "./uploads";
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    private String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".mp4", ".mov"};
}