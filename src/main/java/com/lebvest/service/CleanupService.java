package com.lebvest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j

public class CleanupService {

    private final S3Service s3Service;

    public CleanupService(S3Service s3Service) {
        this.s3Service = s3Service;
    }
    @Async("s3CleanupExecutor")
    public CompletableFuture<Void> deleteByFolder(String prefix) {
        try {
            s3Service.deleteFolderByPrefix(prefix);
        } catch (Exception ex) {
            log.error("Failed to cleanup {}", prefix, ex);
        }
        return CompletableFuture.completedFuture(null);
    }
}