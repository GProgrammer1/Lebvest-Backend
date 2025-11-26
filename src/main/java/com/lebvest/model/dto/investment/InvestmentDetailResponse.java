package com.lebvest.model.dto.investment;

import com.lebvest.model.entities.investment.*;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InvestmentDetailResponse {
    private Long id;
    private String title;
    private String description;
    private InvestmentCategory category;
    private RiskLevel riskLevel;
    private BigDecimal expectedReturn;
    private BigDecimal minInvestment;
    private BigDecimal targetAmount;
    private BigDecimal raisedAmount;
    private Location location;
    private InvestmentType investmentType;
    private Integer durationMonths;
    private String imageUrl;
    private String fundingStage;
    private LocalDate deadline;
    private InvestmentAiPrediction aiPrediction;
    private List<InvestmentHighlight> highlights;
    private List<InvestmentTeamMember> teamMembers;
    private List<InvestmentFinancial> financials;
    private List<InvestmentDocument> documents;
    private List<InvestmentUpdate> updates;
}
