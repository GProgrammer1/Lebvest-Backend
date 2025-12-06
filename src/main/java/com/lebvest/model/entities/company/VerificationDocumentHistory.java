package com.lebvest.model.entities.company;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_document_history",
        indexes = {
                @Index(name = "idx_vdh_company", columnList = "company_id"),
                @Index(name = "idx_vdh_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDocumentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "document_url", length = 512, nullable = false)
    private String documentUrl;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.lebvest.model.enums.VerificationStatus status;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
