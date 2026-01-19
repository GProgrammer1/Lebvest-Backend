package com.lebvest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class CleanupService {

    private final IFileStorageService fileStorageService;

    public CleanupService(IFileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    @Async("s3CleanupExecutor")
    public CompletableFuture<Void> deleteByFolder(String prefix) {
        try {
            fileStorageService.deleteFolderByPrefix(prefix);
        } catch (Exception ex) {
            log.error("Failed to cleanup {}", prefix, ex);
        }
        return CompletableFuture.completedFuture(null);
    }
}