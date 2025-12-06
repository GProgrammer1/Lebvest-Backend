package com.lebvest.model.dto;

import com.lebvest.model.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequestDto {
    private Long id;
    private Long investorId;
    private String investorName;
    private String investorEmail;
    private Long investmentId;
    private String investmentTitle;
    private Long companyId;
    private String companyName;
    private Long investorInvestmentId;
    private BigDecimal amount;
    private BigDecimal expectedReturn;
    private PayoutStatus status;
    private String payoutEvidenceUrl;
    private String adminNotes;
    private String rejectionReason;
    private String stripePayoutId;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
