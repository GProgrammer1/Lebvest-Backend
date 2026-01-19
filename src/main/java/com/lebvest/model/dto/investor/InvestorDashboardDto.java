package com.lebvest.model.dto.investor;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class InvestorDashboardDto {

    private InvestorSummary investor;
    private List<InvestorInvestmentDto> investments;
    private List<InvestmentSummaryDto> watchlist;
    private List<InvestorNotificationDto> notifications;
    private List<InvestorGoalDto> goals;
    private List<InvestmentSummaryDto> recommendations;

    @Getter
    @Builder
    public static class InvestorSummary {
        private Long id;
        private String name;
        private String email;
        private BigDecimal portfolioValue;
        private BigDecimal totalInvested;
        private BigDecimal totalReturns;
        private InvestmentPreferencesDto preferences;
        private Boolean kycVerified;
        private com.lebvest.model.enums.VerificationStatus kycStatus;
    }

    @Getter
    @Builder
    public static class InvestmentPreferencesDto {
        private Set<String> categories;
        private Set<String> riskLevels;
        private Set<String> locations;
    }

    @Getter
    @Builder
    public static class InvestorInvestmentDto {
        private Long id;
        private BigDecimal amount;
        private BigDecimal currentValue;
        private LocalDate investedAt;
        private InvestmentSummaryDto investment;
    }

    @Getter
    @Builder
    public static class InvestmentSummaryDto {
        private Long id;
        private String title;
        private String companyName;
        private String category;
        private String riskLevel;
        private BigDecimal expectedReturn;
        private BigDecimal minInvestment;
        private BigDecimal targetAmount;
        private BigDecimal raisedAmount;
        private String location;
        private String investmentType;
        private Integer durationMonths;
        private String imageUrl;
        private String fundingStage;
        private LocalDate deadline;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class InvestorNotificationDto {
        private Long id;
        private String type;
        private String title;
        private String message;
        private LocalDateTime notifiedAt;
        private boolean read;
        private Long relatedInvestmentId;
    }

    @Getter
    @Builder
    public static class InvestorGoalDto {
        private Long id;
        private String title;
        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private LocalDate deadline;
    }
}
