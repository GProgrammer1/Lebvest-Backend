package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingJob {
    private String jobType; // UPLOAD, PROCESS, VALIDATE
    private Long companyId;
    private Long investmentId;
    private List<String> documentKeys; // S3 keys
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
}

