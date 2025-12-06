package com.lebvest.model.dto;

import com.lebvest.model.enums.InvestmentRequestStatus;
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
public class InvestmentRequestDto {
    private Long id;
    private Long investorId;
    private String investorName;
    private String investorEmail;
    private String investorProfileImageUrl;
    private Long investmentId;
    private String investmentTitle;
    private BigDecimal amount;
    private InvestmentRequestStatus status;
    private String rejectionReason;
    private String message;
    private String stripePaymentIntentId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
}
