package com.lebvest.model.dto;

import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentDto {
    private Long id;
    private String title;
    private String companyName;
    private String description;
    private InvestmentCategory category;
    private RiskLevel riskLevel;
    private BigDecimal expectedReturn; // percentage
    private BigDecimal minInvestment; // in USD
    private BigDecimal targetAmount; // in USD
    private BigDecimal raisedAmount; // in USD
    private Location location;
    private String sector; // CompanySector as string
    private InvestmentType investmentType;
    private Integer duration; // in months
    private String imageUrl;
    private List<String> highlights;
    private AiPredictionDto aiPrediction;
    private String fundingStage;
    private LocalDate deadline;
    private LocalDateTime createdAt;
    private List<TeamMemberDto> team;
    private List<FinancialDto> financials;
    private List<DocumentDto> documents;
    private List<UpdateDto> updates;
    private Boolean isInWatchlist; // For authenticated users

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiPredictionDto {
        private BigDecimal profitPrediction; // percentage
        private String riskAssessment;
        private BigDecimal confidenceScore; // 0-100
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberDto {
        private String name;
        private String role;
        private String bio;
        private String imageUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialDto {
        private BigDecimal revenue;
        private BigDecimal expenses;
        private BigDecimal profit;
        private Integer year;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDto {
        private String title;
        private String type;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateDto {
        private LocalDate date;
        private String title;
        private String content;
    }
}



