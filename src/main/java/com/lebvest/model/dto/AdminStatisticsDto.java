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
public class AdminStatisticsDto {
    private Long totalCompanies;
    private Long totalInvestors;
    private Long totalInvestments;
    private Long activeInvestments; // Investments with deadline in future
    private BigDecimal totalRaisedAmount;
    private BigDecimal totalTargetAmount;
    private Long totalInvestorInvestments; // Total number of investments made by investors
    private Long pendingSignupRequests;
}

