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
            
            // Get project root directory
            String projectRoot = System.getProperty("user.dir");
            Path fullPath = Paths.get(projectRoot, filePath);
            
            // Security check: ensure the file is within the uploads directory
            Path uploadsDir = Paths.get(projectRoot, "uploads");
            if (!fullPath.normalize().startsWith(uploadsDir.normalize())) {
                log.warn("Attempted access to file outside uploads directory: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            File file = fullPath.toFile();
            
            if (!file.exists() || !file.isFile()) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
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

