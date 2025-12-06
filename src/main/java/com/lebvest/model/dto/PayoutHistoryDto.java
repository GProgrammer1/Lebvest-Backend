package com.lebvest.model.dto;

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
public class PayoutHistoryDto {
    private Long id;
    private Long investorId;
    private String investorName;
    private Long investmentId;
    private String investmentTitle;
    private Long payoutRequestId;
    private BigDecimal principalAmount;
    private BigDecimal returnAmount;
    private BigDecimal totalPayout;
    private String payoutMethod;
    private String transactionId;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
