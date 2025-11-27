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
public class InvestmentStatsDto {
    private Long investmentId;
    private String investmentTitle;
    private BigDecimal targetAmount;
    private BigDecimal raisedAmount;
    private BigDecimal progressPercentage; // (raisedAmount / targetAmount) * 100
    private Integer totalInvestors;
    private BigDecimal averageInvestmentAmount;
    private BigDecimal minInvestmentAmount;
    private BigDecimal maxInvestmentAmount;
}

