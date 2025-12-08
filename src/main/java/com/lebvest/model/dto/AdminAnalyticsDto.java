package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsDto {
    // Overview stats
    private Long totalInvestors;
    private Long totalCompanies;
    private Long totalInvestments;
    private BigDecimal totalInvestedToday;
    private BigDecimal totalInvestedThisMonth;
    
    // Top projects
    private List<TopProjectDto> topProjects;
    
    // Time series data for charts
    private Map<LocalDate, BigDecimal> dailyInvestments; // Last 30 days
    private Map<String, Long> investmentsByCategory;
    private Map<String, Long> investmentsByRiskLevel;
    
    // Queue counts
    private Long pendingCompanyApprovals;
    private Long pendingInvestorApprovals;
    private Long pendingPayouts;
    private Long pendingReturns;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProjectDto {
        private Long id;
        private String title;
        private String companyName;
        private BigDecimal raisedAmount;
        private BigDecimal targetAmount;
        private Long investorCount;
    }
}

