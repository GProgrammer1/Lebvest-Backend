package com.lebvest.model.dto;

import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentStatus;
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
public class AdminProjectReviewDto {
    private Long id;
    private String title;
    private String companyName;
    private Long companyId;
    private String description;
    private InvestmentCategory category;
    private RiskLevel riskLevel;
    private BigDecimal expectedReturn;
    private BigDecimal minInvestment;
    private BigDecimal targetAmount;
    private BigDecimal raisedAmount;
    private Location location;
    private String sector;
    private InvestmentType investmentType;
    private Integer durationMonths;
    private String imageUrl;
    private String fundingStage;
    private LocalDate deadline;
    private LocalDateTime createdAt;
    private LocalDateTime submittedDate;
    private InvestmentStatus status;
    private List<String> highlights;
    private List<TeamMemberDto> team;
    private List<FinancialDto> financials;
    private List<DocumentDto> documents;
    private List<UpdateDto> updates;

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

