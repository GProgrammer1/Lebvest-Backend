package com.lebvest.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action_type"),
        @Index(name = "idx_audit_timestamp", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType; // LOGIN, LOGOUT, ADMIN_ACTION, FUNDING_OPERATION, etc.

    @Column(name = "resource_type", length = 100)
    private String resourceType; // USER, COMPANY, INVESTMENT, etc.

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Lob
    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON string with additional details

    @Column(name = "status", length = 50)
    private String status; // SUCCESS, FAILURE, ERROR

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

