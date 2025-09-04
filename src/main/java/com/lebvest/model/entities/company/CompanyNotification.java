package com.lebvest.model.entities.company;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.enums.CompanyNotificationType;
import com.lebvest.model.enums.InvestorNotificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification messages sent to companies.
 */
@Entity
@Table(
        name = "company_notifications",
        indexes = @Index(name = "idx_company_notifs", columnList = "company_id")
)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@SuperBuilder
public class CompanyNotification {

    @Id
    @Column(length = 36)
    private String id;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyNotificationType type;

    @NonNull
    @Column(nullable = false, length = 255)
    private String title;

    @NonNull
    @Lob
    @Column(nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "notified_at", updatable = false)
    private LocalDateTime notifiedAt;

    @NonNull
    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_investment_id")
    private Investment relatedInvestment;
}
