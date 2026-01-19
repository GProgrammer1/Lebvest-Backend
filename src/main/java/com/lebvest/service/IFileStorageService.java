package com.lebvest.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;


public interface IFileStorageService {

    String uploadFile(String prefix, String fileName, InputStream inputStream, long contentLength, String contentType);


    List<String> listFilesByPrefix(String prefix);

    void deleteFolderByPrefix(String prefix);


    void moveFilesAndDelete(List<String> keys, String acceptedPrefix);

    void uploadPendingDocs(UUID requestId, MultipartFile[] files);

    List<String> savePendingFiles(UUID requestId, MultipartFile[] files);

    List<String> moveToAccepted(UUID requestId, List<String> pendingPaths);

    void deletePendingFiles(UUID requestId);


    String savePayoutEvidence(Long payoutRequestId, MultipartFile evidenceFile);
}

