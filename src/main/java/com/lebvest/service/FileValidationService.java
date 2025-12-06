package com.lebvest.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    ));
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = new HashSet<>(Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ));
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".jpeg", ".png", ".gif", ".webp"
    ));

    private final Tika tika;

    public FileValidationService() {
        this.tika = new Tika(new DefaultDetector());
    }

    /**
     * Validates a file upload for security and content type
     * @param file The file to validate
     * @param isImage Whether this should be an image file
     * @throws IllegalArgumentException if validation fails
     */
    public void validateFile(MultipartFile file, boolean isImage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and cannot be empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        // Sanitize filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File must have a valid filename");
        }

        // Check for path traversal attempts
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename: path traversal not allowed");
        }

        // Check file extension
        String lowerFilename = originalFilename.toLowerCase();
        boolean hasValidExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(lowerFilename::endsWith);
        if (!hasValidExtension) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
        }

        // Validate actual content type using Tika
        try (InputStream inputStream = file.getInputStream();
             TikaInputStream tikaInputStream = TikaInputStream.get(inputStream)) {
            
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, originalFilename);
            MediaType detectedType = tika.getDetector().detect(tikaInputStream, metadata);
            String detectedMimeType = detectedType.toString();

            log.info("File validation - Original: {}, Detected: {}, Expected Image: {}", 
                    file.getContentType(), detectedMimeType, isImage);

            // Validate against detected MIME type
            if (isImage) {
                if (!ALLOWED_IMAGE_TYPES.contains(detectedMimeType)) {
                    throw new IllegalArgumentException("File is not a valid image. Detected type: " + detectedMimeType);
                }
            } else {
                // For documents, allow both document and image types (some documents might be scanned images)
                if (!ALLOWED_DOCUMENT_TYPES.contains(detectedMimeType) && 
                    !ALLOWED_IMAGE_TYPES.contains(detectedMimeType)) {
                    throw new IllegalArgumentException("File type not allowed. Detected type: " + detectedMimeType);
                }
            }

            // Additional security: Check for executable content
            if (detectedMimeType.startsWith("application/x-executable") ||
                detectedMimeType.startsWith("application/x-msdownload") ||
                detectedMimeType.contains("script")) {
                throw new IllegalArgumentException("Executable files are not allowed");
            }

        } catch (IOException e) {
            log.error("Error validating file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to validate file: " + e.getMessage());
        }
    }

    /**
     * Sanitizes a filename by removing dangerous characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "file";
        }

        // Remove path components
        String sanitized = filename.replaceAll(".*[/\\\\]", "");
        
        // Remove dangerous characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Limit length
        if (sanitized.length() > 255) {
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                String ext = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, 255 - ext.length()) + ext;
            } else {
                sanitized = sanitized.substring(0, 255);
            }
        }

        return sanitized;
    }
}

