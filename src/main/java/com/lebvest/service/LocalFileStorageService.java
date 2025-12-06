package com.lebvest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${file.upload.base-dir:}")
    private String configuredBaseDir;
    
    /**
     * Get the absolute base directory for uploads.
     * Uses configured path if set, otherwise resolves to a consistent location
     * based on the JAR location or project root.
     */
    private Path getUploadsBaseDirectory() {
        if (configuredBaseDir != null && !configuredBaseDir.trim().isEmpty()) {
            Path configuredPath = Paths.get(configuredBaseDir).toAbsolutePath().normalize();
            log.info("Using configured uploads base directory: {}", configuredPath);
            return configuredPath;
        }
        
        // Try to find the project root by looking for the uploads directory
        // Start from user.dir and work backwards if needed
        String userDir = System.getProperty("user.dir");
        Path projectRoot = Paths.get(userDir).toAbsolutePath().normalize();
        
        // Check if uploads exists in current directory
        Path uploadsDir = projectRoot.resolve(UPLOADS_BASE_DIR);
        if (Files.exists(uploadsDir)) {
            log.info("Found uploads directory at: {}", uploadsDir);
            return uploadsDir;
        }
        
        // Try parent directory (in case we're in a subdirectory like lebvest-backend)
        Path parentDir = projectRoot.getParent();
        if (parentDir != null) {
            Path parentUploads = parentDir.resolve(UPLOADS_BASE_DIR);
            if (Files.exists(parentUploads)) {
                log.info("Found uploads directory in parent: {}", parentUploads);
                return parentUploads;
            }
        }
        
        // If not found, use the project root (will create uploads there)
        Path defaultUploads = projectRoot.resolve(UPLOADS_BASE_DIR);
        log.info("Using default uploads directory: {}", defaultUploads);
        return defaultUploads;
    }

    /**
     * Save files to local storage in pending folder structure
     * @param requestId The UUID of the signup request
     * @param files Array of files to save
     * @return List of relative file paths (e.g., "uploads/pending/{requestId}/filename.pdf")
     */
    public List<String> savePendingFiles(UUID requestId, MultipartFile[] files) {
        log.info("=== LocalFileStorageService.savePendingFiles START ===");
        log.info("RequestId: {}", requestId);
        log.info("Number of files received: {}", files != null ? files.length : 0);
        
        if (files == null || files.length == 0) {
            log.warn("No files provided to savePendingFiles");
            return new ArrayList<>();
        }

        List<String> savedPaths = new ArrayList<>();
        
        try {
            // Get consistent uploads base directory
            Path uploadsBase = getUploadsBaseDirectory();
            log.info("Uploads base directory: {}", uploadsBase.toAbsolutePath());
            
            // Create directory structure: uploads/pending/{requestId}/
            Path pendingDir = uploadsBase.resolve(PENDING_DIR).resolve(requestId.toString());
            log.info("Target directory path: {}", pendingDir.toAbsolutePath());
            
            Files.createDirectories(pendingDir);
            log.info("Directory created/verified: {}", pendingDir.toAbsolutePath());
            
            // Verify directory was created
            if (!Files.exists(pendingDir)) {
                log.error("Failed to create directory: {}", pendingDir.toAbsolutePath());
                throw new IOException("Failed to create directory: " + pendingDir.toAbsolutePath());
            }
            log.info("Directory exists: {}", Files.exists(pendingDir));

            int fileIndex = 0;
            for (MultipartFile file : files) {
                fileIndex++;
                if (file == null || file.isEmpty()) {
                    log.warn("File {} is null or empty, skipping", fileIndex);
                    continue;
                }

                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null || originalFileName.isEmpty()) {
                    originalFileName = "file";
                }
                
                log.info("Processing file {}: originalName={}, size={}, contentType={}", 
                        fileIndex, originalFileName, file.getSize(), file.getContentType());

                // Sanitize filename and create unique name
                String sanitizedFileName = sanitizeFileName(originalFileName);
                String uniqueFileName = System.currentTimeMillis() + "_" + sanitizedFileName;
                log.info("Generated unique filename: {}", uniqueFileName);

                // Save file
                Path targetPath = pendingDir.resolve(uniqueFileName);
                log.info("Target file path: {}", targetPath.toAbsolutePath());
                
                Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // Verify file was saved
                if (!Files.exists(targetPath)) {
                    log.error("File was not saved! Target path: {}", targetPath.toAbsolutePath());
                    throw new IOException("File was not saved: " + targetPath.toAbsolutePath());
                }
                
                long fileSize = Files.size(targetPath);
                log.info("File saved successfully! Path: {}, Size: {} bytes", targetPath.toAbsolutePath(), fileSize);

                // Store relative path for database
                String relativePath = UPLOADS_BASE_DIR + "/" + PENDING_DIR + "/" + requestId.toString() + "/" + uniqueFileName;
                savedPaths.add(relativePath);
                
                log.info("Relative path for database: {}", relativePath);
            }
            
            log.info("=== LocalFileStorageService.savePendingFiles END ===");
            log.info("Total files saved: {}", savedPaths.size());
            log.info("Saved paths: {}", savedPaths);

        } catch (IOException e) {
            log.error("=== LocalFileStorageService.savePendingFiles FAILED ===");
            log.error("RequestId: {}", requestId);
            log.error("Error: {}", e.getMessage(), e);
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
        log.info("=== LocalFileStorageService.moveToAccepted START ===");
        log.info("RequestId: {}", requestId);
        log.info("Number of pending paths: {}", pendingPaths != null ? pendingPaths.size() : 0);
        
        if (pendingPaths == null || pendingPaths.isEmpty()) {
            log.warn("No pending paths provided to moveToAccepted");
            return new ArrayList<>();
        }

        List<String> acceptedPaths = new ArrayList<>();
        
        try {
            // Get consistent uploads base directory
            Path uploadsBase = getUploadsBaseDirectory();
            log.info("Uploads base directory: {}", uploadsBase.toAbsolutePath());
            
            Path acceptedDir = uploadsBase.resolve("accepted").resolve(requestId.toString());
            log.info("Accepted directory path: {}", acceptedDir.toAbsolutePath());
            
            Files.createDirectories(acceptedDir);
            log.info("Accepted directory created/verified: {}", acceptedDir.toAbsolutePath());

            int movedCount = 0;
            int skippedCount = 0;
            
            for (String pendingPath : pendingPaths) {
                // Resolve relative path against uploads base directory
                Path sourcePath = uploadsBase.resolve(pendingPath.replace(UPLOADS_BASE_DIR + "/", ""));
                log.info("Processing pending path: {} -> {}", pendingPath, sourcePath.toAbsolutePath());
                
                if (!Files.exists(sourcePath)) {
                    log.error("Pending file not found: {} (absolute: {})", pendingPath, sourcePath.toAbsolutePath());
                    skippedCount++;
                    continue;
                }

                String fileName = sourcePath.getFileName().toString();
                Path targetPath = acceptedDir.resolve(fileName);
                
                log.info("Moving file: {} -> {}", sourcePath.toAbsolutePath(), targetPath.toAbsolutePath());
                
                Files.move(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // Verify file was moved
                if (!Files.exists(targetPath)) {
                    log.error("File was not moved! Target path: {}", targetPath.toAbsolutePath());
                } else {
                    log.info("File successfully moved. Target exists: {}, Size: {} bytes", 
                            targetPath.toAbsolutePath(), Files.size(targetPath));
                }
                
                String relativePath = UPLOADS_BASE_DIR + "/accepted/" + requestId.toString() + "/" + fileName;
                acceptedPaths.add(relativePath);
                movedCount++;
                
                log.info("File moved successfully. Relative path: {}", relativePath);
            }
            
            log.info("=== LocalFileStorageService.moveToAccepted END ===");
            log.info("Files moved: {}, Files skipped: {}, Total accepted paths: {}", 
                    movedCount, skippedCount, acceptedPaths.size());

        } catch (IOException e) {
            log.error("=== LocalFileStorageService.moveToAccepted FAILED ===");
            log.error("RequestId: {}", requestId);
            log.error("Error: {}", e.getMessage(), e);
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
            // Get consistent uploads base directory
            Path uploadsBase = getUploadsBaseDirectory();
            Path pendingDir = uploadsBase.resolve(PENDING_DIR).resolve(requestId.toString());
            
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

