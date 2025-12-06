package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutCalculationJob {
    private Long investmentId;
    private Long investorId;
    private BigDecimal amount;
    private String calculationType; // MONTHLY, QUARTERLY, ANNUAL, FINAL
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
}

