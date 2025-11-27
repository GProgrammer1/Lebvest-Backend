package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDashboardDto {
    private CompanyProfileDto companyProfile;
    private DashboardStatsDto stats;
    private List<InvestmentDto> recentInvestments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsDto {
        private Long totalInvestments;
        private Long activeInvestments;
        private BigDecimal totalRaised;
        private BigDecimal totalTarget;
        private Long totalInvestors;
        private BigDecimal averageInvestmentAmount;
    }
}

