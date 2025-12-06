package com.lebvest.model.entities.investment;

import com.lebvest.model.entities.investor.Investor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_history",
        indexes = {
                @Index(name = "idx_ph_investor", columnList = "investor_id"),
                @Index(name = "idx_ph_investment", columnList = "investment_id"),
                @Index(name = "idx_ph_payout_request", columnList = "payout_request_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payout_request_id", nullable = false)
    private PayoutRequest payoutRequest;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal returnAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayout;

    @Column(name = "payout_method", length = 50)
    private String payoutMethod; // "STRIPE", "BANK_TRANSFER", "MANUAL", etc.

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
