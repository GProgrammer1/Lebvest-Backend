package com.lebvest.model.dto;

import jakarta.mail.Multipart;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public record CompanySignupUploadEvent(UUID requestId, MultipartFile[] files) {
}
