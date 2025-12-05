package com.lebvest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService {

    private static final String UPLOADS_BASE_DIR = "uploads";
    private static final String PENDING_DIR = "pending";

    /**
     * Save files to local storage in pending folder structure
     * @param requestId The UUID of the signup request
     * @param files Array of files to save
     * @return List of relative file paths (e.g., "uploads/pending/{requestId}/filename.pdf")
     */
    public List<String> savePendingFiles(UUID requestId, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        List<String> savedPaths = new ArrayList<>();
        
        try {
            // Create directory structure: uploads/pending/{requestId}/
            String projectRoot = System.getProperty("user.dir");
            Path pendingDir = Paths.get(projectRoot, UPLOADS_BASE_DIR, PENDING_DIR, requestId.toString());
            Files.createDirectories(pendingDir);
            
            log.info("Saving files to: {}", pendingDir.toAbsolutePath());

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null || originalFileName.isEmpty()) {
                    originalFileName = "file";
                }

                // Sanitize filename and create unique name
                String sanitizedFileName = sanitizeFileName(originalFileName);
                String uniqueFileName = System.currentTimeMillis() + "_" + sanitizedFileName;

                // Save file
                Path targetPath = pendingDir.resolve(uniqueFileName);
                Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Store relative path for database
                String relativePath = UPLOADS_BASE_DIR + "/" + PENDING_DIR + "/" + requestId.toString() + "/" + uniqueFileName;
                savedPaths.add(relativePath);
                
                log.info("File saved: {}", relativePath);
            }

        } catch (IOException e) {
            log.error("Failed to save pending files: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save files: " + e.getMessage(), e);
        }

        return savedPaths;
    }

    /**
     * Move files from pending to accepted folder
     * @param requestId The UUID of the signup request
     * @param pendingPaths List of pending file paths
     * @return List of accepted file paths
     */
    public List<String> moveToAccepted(UUID requestId, List<String> pendingPaths) {
        if (pendingPaths == null || pendingPaths.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> acceptedPaths = new ArrayList<>();
        String projectRoot = System.getProperty("user.dir");

        try {
            Path acceptedDir = Paths.get(projectRoot, UPLOADS_BASE_DIR, "accepted", requestId.toString());
            Files.createDirectories(acceptedDir);

            for (String pendingPath : pendingPaths) {
                Path sourcePath = Paths.get(projectRoot, pendingPath);
                
                if (!Files.exists(sourcePath)) {
                    log.warn("Pending file not found: {}", sourcePath);
                    continue;
                }

                String fileName = sourcePath.getFileName().toString();
                Path targetPath = acceptedDir.resolve(fileName);
                
                Files.move(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                String relativePath = UPLOADS_BASE_DIR + "/accepted/" + requestId.toString() + "/" + fileName;
                acceptedPaths.add(relativePath);
                
                log.info("File moved from pending to accepted: {}", relativePath);
            }

        } catch (IOException e) {
            log.error("Failed to move files to accepted: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to move files: " + e.getMessage(), e);
        }

        return acceptedPaths;
    }

    /**
     * Delete pending files
     * @param requestId The UUID of the signup request
     */
    public void deletePendingFiles(UUID requestId) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path pendingDir = Paths.get(projectRoot, UPLOADS_BASE_DIR, PENDING_DIR, requestId.toString());
            
            if (Files.exists(pendingDir)) {
                Files.walk(pendingDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Failed to delete file: {}", path, e);
                            }
                        });
                log.info("Deleted pending files for request: {}", requestId);
            }
        } catch (IOException e) {
            log.error("Failed to delete pending files: {}", e.getMessage(), e);
        }
    }

    private String sanitizeFileName(String fileName) {
        // Remove or replace invalid characters
        return fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}

