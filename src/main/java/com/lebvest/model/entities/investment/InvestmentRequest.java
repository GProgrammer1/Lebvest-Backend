package com.lebvest.model.entities.investment;

import com.lebvest.model.enums.InvestmentRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an investment request from an investor to a company.
 * The company must accept before the investor can proceed with payment.
 */
@Entity
@Table(name = "investment_requests",
        indexes = {
                @Index(name = "idx_ir_investor", columnList = "investor_id"),
                @Index(name = "idx_ir_investment", columnList = "investment_id"),
                @Index(name = "idx_ir_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private com.lebvest.model.entities.investor.Investor investor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvestmentRequestStatus status = InvestmentRequestStatus.PENDING;

    @Column(length = 1000)
    private String rejectionReason; // Reason provided by company when rejecting

    @Column(length = 1000)
    private String message; // Optional message from investor

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId; // Stripe payment intent ID after acceptance

    @Column(name = "paid_at")
    private LocalDateTime paidAt; // When payment was completed

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt; // When company accepted

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt; // When company rejected
}
