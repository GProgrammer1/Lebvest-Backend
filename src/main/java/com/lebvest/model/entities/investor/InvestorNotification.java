package com.lebvest.model.entities.investor;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.enums.InvestorNotificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "investor_notifications")
@RequiredArgsConstructor
@Data
public class InvestorNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "investor_id",
            referencedColumnName = "id",
            unique = true,       // enforces one-to-one at the DB level
            nullable = false
    )    private Investor investor;

    @Column(nullable = false)
    private InvestorNotificationType investorNotificationType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @CreationTimestamp
    @Column(updatable = false, name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(nullable = false, name = "is_read")
    private boolean isRead = false;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "related_investment_id",
    unique = true,
    nullable = false,
    referencedColumnName = "id")
    private Investment relatedInvestment;



}
