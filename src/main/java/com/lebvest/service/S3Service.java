package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.entities.company.CompanySignupRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3AsyncClient s3Async;
    private final String BUCKET_NAME;
    private final VarsConfig varsConfig;

    public S3Service(S3Client s3Client, VarsConfig varsConfig, S3Presigner s3Presigner, S3AsyncClient s3Async) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3Async = s3Async;
        this.varsConfig = varsConfig;
        BUCKET_NAME = varsConfig.getAwsBucket();
    }
    public String uploadFile(String prefix, String fileName, InputStream inputStream, long contentLength, String contentType) {
        String key = prefix + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));

        return key; // Return the uploaded file's key (can also generate public URL)
    }

    public List<String> listFilesByPrefix(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(BUCKET_NAME)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        List<String> keys = new ArrayList<>();
        for (S3Object obj : response.contents()) {
            keys.add(obj.key());
        }

        return keys;
    }
//
//    public String generatePresignedUrl(String key) {
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                .bucket(BUCKET_NAME)
//                .key(key)
//                .build();
//
//        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                .getObjectRequest(getObjectRequest)
//                .signatureDuration(Duration.ofMinutes(15))
//                .build();
//
//        return s3Presigner.presignGetObject(presignRequest).url().toString();
//    }

//    public void moveFile(String sourceKey, String destinationKey) {
//        // 1. Copy to new location
//        s3Client.copyObject(builder -> builder
//                .sourceBucket(BUCKET_NAME)
//                .sourceKey(sourceKey)
//                .destinationBucket(BUCKET_NAME)
//                .destinationKey(destinationKey)
//        );
//
////        // 2. Delete old file
////        s3Client.deleteObject(builder -> builder
////                .bucket(BUCKET_NAME)
////                .key(sourceKey)
////        );
//    }


    public void deleteFolderByPrefix(String prefix) {
        List<String> keys = listFilesByPrefix(prefix);

        if (keys.isEmpty()) return;

        List<ObjectIdentifier> objectsToDelete = keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

        s3Client.deleteObjects(builder -> builder
                .bucket(BUCKET_NAME)
                .delete(d -> d.objects(objectsToDelete))
        );
    }

    @Async("taskExecutor")
    public void moveFilesAndDelete(List<String> keys, String acceptedPrefix) {
        List<CompletableFuture<Void>> operations = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<String> failedKeys = Collections.synchronizedList(new ArrayList<>());

        for (String oldKey : keys) {
            String fileName = extractFileName(oldKey);
            String newKey = acceptedPrefix + "/" + fileName;
            log.info("🔐 File name: {}", fileName);
            log.info("📁 Old key: {}", oldKey);
            log.info("📂 New key: {}", newKey);

            // Proper encoding: space => %20 (not +), keep slashes
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%28", "(")   // OPTIONAL: remove this if needed
                    .replace("%29", ")");  // OPTIONAL: remove this if needed

            String path = oldKey.substring(0, oldKey.lastIndexOf("/") + 1);
            String copySrc = BUCKET_NAME + "/" + encodeKeyPreservingSlashes(oldKey);

            log.info("🛠️ Copy src: {}", copySrc);

            // 🔍 Verify file exists in S3
            ListObjectsV2Response verify = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(BUCKET_NAME)
                    .prefix(oldKey.substring(0, oldKey.lastIndexOf("/") + 1))
                    .build());

            if (verify.contents().isEmpty()) {
                log.error("❌ S3 VERIFY FAIL: File '{}' DOES NOT EXIST in S3!", oldKey);
            } else {
                log.info("✅ S3 VERIFY SUCCESS: File exists.");
            }

            CompletableFuture<Void> operation = s3Async.copyObject(
                            CopyObjectRequest.builder()
                                    .copySource(copySrc)
                                    .destinationBucket(BUCKET_NAME)
                                    .destinationKey(newKey)
                                    .build()
                    )
                    .thenCompose(copyResp ->
                            s3Async.deleteObject(DeleteObjectRequest.builder()
                                    .bucket(BUCKET_NAME)
                                    .key(oldKey)
                                    .build())
                    )
                    .thenAccept(delResp -> log.info("✅ Transferred & deleted: {}", oldKey))
                    .exceptionally(ex -> {
                        failedKeys.add(oldKey);

                        log.error("❌ Error copying/deleting '{}': {}", oldKey, ex.getMessage());
                        return null;
                    });


            operations.add(operation);
        }

        CompletableFuture<Void> allFuture =  CompletableFuture
                .allOf(operations.toArray(new CompletableFuture[0]))
                .whenComplete((res, ex) -> {
                    executor.shutdown();
                    if (ex != null) {
                        log.error("❌ Overall failure during file move: {}", ex.getMessage(), ex);
                    } else {
                        log.info("✅ All file move operations completed.");
                    }
                });

        if (!failedKeys.isEmpty()) {
            log.warn("⚠️ Failed to process {} files: {}", failedKeys.size(), failedKeys);
        }

    }

    private String encodeKeyPreservingSlashes(String key) {
        return Arrays.stream(key.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8)
                        .replace("+", "%20")) // encode space as %20, not +
                .reduce((a, b) -> a + "/" + b)
                .orElse("");
    }




    private String extractFileName(String key) {
        int idx = key.lastIndexOf('/');
        return (idx >= 0 ? key.substring(idx + 1) : key);
    }

    @Async("taskExecutor")
    public void uploadPendingDocs(
            UUID requestId,
            MultipartFile[] files
            ) {
        ExecutorService executor = Executors.newFixedThreadPool(8); // limit to 3 threads

        String prefix = varsConfig.getPendingPrefix(requestId);
        List<CompletableFuture<String>> uploadFutures =
                Arrays.stream(files)
                        .map(file -> CompletableFuture.supplyAsync(() -> {
                            try (InputStream inputStream = file.getInputStream()) {

                                return
                                        this.uploadFile(
                                        prefix,
                                        file.getOriginalFilename(),
                                        inputStream,
                                        file.getSize(),
                                        file.getContentType()
                                );
                            } catch (IOException e) {
                                throw new CompletionException(
                                        new RuntimeException("Failed to upload document: " + file.getOriginalFilename(), e)
                                );
                            }
                        },executor).exceptionally((ex) -> {
                            log.error("Error uploading document: {}", ex.getMessage());
                            return null;
                        }))
                        .toList();


    }


}
