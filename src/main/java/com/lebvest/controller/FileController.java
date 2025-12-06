package com.lebvest.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        try {
            // Extract the file path from the request
            String requestPath = request.getRequestURI();
            String filePath = requestPath.replace("/api/files/", "");
            
            log.debug("File request path: {}", filePath);
            
            // Get project root directory - try user.dir first, then check if uploads exists
            String projectRoot = System.getProperty("user.dir");
            log.debug("Project root (user.dir): {}", projectRoot);
            
            // Verify uploads directory exists, if not try parent directory
            Path uploadsDir = Paths.get(projectRoot, "uploads").normalize();
            if (!Files.exists(uploadsDir)) {
                // Try parent directory (in case we're in a subdirectory)
                Path projectRootPath = Paths.get(projectRoot);
                Path parent = projectRootPath.getParent();
                if (parent != null) {
                    Path parentUploads = parent.resolve("uploads").normalize();
                    if (Files.exists(parentUploads)) {
                        projectRoot = parent.toString();
                        uploadsDir = parentUploads;
                        log.debug("Using parent directory as project root: {}", projectRoot);
                    }
                }
                // If still not found, try relative to current working directory
                if (!Files.exists(uploadsDir)) {
                    Path currentUploads = Paths.get("uploads").toAbsolutePath().normalize();
                    if (Files.exists(currentUploads)) {
                        projectRoot = currentUploads.getParent().toString();
                        uploadsDir = currentUploads;
                        log.debug("Using current working directory as project root: {}", projectRoot);
                    }
                }
            }
            
            Path fullPath = Paths.get(projectRoot, filePath).normalize();
            log.debug("Full file path: {}", fullPath.toAbsolutePath());
            log.debug("Uploads directory: {}", uploadsDir.toAbsolutePath());
            
            // Security check: ensure the file is within the uploads directory
            if (!fullPath.startsWith(uploadsDir)) {
                log.warn("Attempted access to file outside uploads directory. Requested: {}, Uploads dir: {}", 
                        fullPath.toAbsolutePath(), uploadsDir.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            File file = fullPath.toFile();
            
            if (!file.exists()) {
                log.warn("File not found at path: {}", fullPath.toAbsolutePath());
                
                // Check if parent directory exists
                File parentDir = file.getParentFile();
                if (parentDir != null) {
                    if (!parentDir.exists()) {
                        log.warn("Parent directory does not exist: {}", parentDir.getAbsolutePath());
                    } else {
                        log.debug("Parent directory exists: {}", parentDir.getAbsolutePath());
                        // List files in parent directory for debugging
                        File[] filesInDir = parentDir.listFiles();
                        if (filesInDir != null && filesInDir.length > 0) {
                            log.debug("Files in parent directory (showing first 5): {}", 
                                    java.util.Arrays.stream(filesInDir)
                                            .limit(5)
                                            .map(File::getName)
                                            .collect(java.util.stream.Collectors.joining(", ")));
                        } else {
                            log.warn("Parent directory is empty: {}", parentDir.getAbsolutePath());
                        }
                    }
                }
                
                // If file is in pending but not found, check if it was moved to accepted
                if (filePath.contains("/pending/")) {
                    String acceptedPath = filePath.replace("/pending/", "/accepted/");
                    Path acceptedFullPath = Paths.get(projectRoot, acceptedPath).normalize();
                    File acceptedFile = acceptedFullPath.toFile();
                    
                    if (acceptedFile.exists() && acceptedFile.isFile()) {
                        log.debug("File found in accepted directory: {}", acceptedFullPath.toAbsolutePath());
                        file = acceptedFile;
                        fullPath = acceptedFullPath;
                    } else {
                        log.warn("File not found in pending or accepted. Requested: {}, Checked accepted: {}", 
                                fullPath.toAbsolutePath(), acceptedFullPath.toAbsolutePath());
                        return ResponseEntity.notFound().build();
                    }
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
            
            if (!file.isFile()) {
                log.warn("Path exists but is not a file: {}", fullPath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            log.debug("Serving file: {}", fullPath.toAbsolutePath());
            Resource resource = new FileSystemResource(file);
            
            // Determine content type
            String contentType = Files.probeContentType(fullPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error serving file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

