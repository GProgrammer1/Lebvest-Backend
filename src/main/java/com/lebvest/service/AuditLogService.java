package com.lebvest.service;

import com.lebvest.model.entities.AuditLog;
import com.lebvest.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async("taskExecutor")
    @Transactional
    public void logAction(String actionType, String resourceType, Long resourceId, Long userId, 
                         String userEmail, HttpServletRequest request, String status, String errorMessage, 
                         Map<String, Object> additionalDetails) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actionType(actionType)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .userId(userId)
                    .userEmail(userEmail)
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .status(status)
                    .errorMessage(errorMessage)
                    .details(additionalDetails != null ? convertToJson(additionalDetails) : null)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {}", actionType, userEmail);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
            // Don't throw exception - audit logging should not break the main flow
        }
    }

    public void logLoginAttempt(String userEmail, HttpServletRequest request, String status, String errorMessage) {
        logAction("LOGIN_ATTEMPT", "USER", null, null, userEmail, request, status, errorMessage, null);
    }

    public void logAdminAction(Long adminId, String adminEmail, String actionType, String resourceType, 
                               Long resourceId, HttpServletRequest request, Map<String, Object> details) {
        logAction(actionType, resourceType, resourceId, adminId, adminEmail, request, "SUCCESS", null, details);
    }

    public void logFundingOperation(Long userId, String userEmail, String operationType, Long investmentId, 
                                    HttpServletRequest request, Map<String, Object> details) {
        logAction("FUNDING_OPERATION", "INVESTMENT", investmentId, userId, userEmail, request, "SUCCESS", null, details);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    public Page<AuditLog> getAuditLogsByActionType(String actionType, Pageable pageable) {
        return auditLogRepository.findByActionType(actionType, pageable);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String convertToJson(Map<String, Object> details) {
        // Simple JSON conversion - in production, use Jackson ObjectMapper
        if (details == null || details.isEmpty()) {
            return null;
        }
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}

