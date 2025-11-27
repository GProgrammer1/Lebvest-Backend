package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentDetailDto {
    private InvestmentDto investment;
    private List<InvestorInfoDto> investors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestorInfoDto {
        private Long investorId;
        private String investorName;
        private String investorEmail;
        private BigDecimal investedAmount;
        private BigDecimal currentValue;
        private LocalDate investedAt;
    }
}

