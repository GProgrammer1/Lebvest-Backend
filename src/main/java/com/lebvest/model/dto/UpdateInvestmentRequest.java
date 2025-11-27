package com.lebvest.model.dto;

import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import jakarta.validation.constraints.*;
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
public class UpdateInvestmentRequest {
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private InvestmentCategory category;

    private RiskLevel riskLevel;

    @DecimalMin(value = "0.0", message = "Expected return must be positive")
    @DecimalMax(value = "100.0", message = "Expected return must not exceed 100%")
    private BigDecimal expectedReturn;

    @DecimalMin(value = "0.01", message = "Minimum investment must be positive")
    private BigDecimal minInvestment;

    @DecimalMin(value = "0.01", message = "Target amount must be positive")
    private BigDecimal targetAmount;

    private Location location;

    private InvestmentType investmentType;

    @Min(value = 1, message = "Duration must be at least 1 month")
    private Integer durationMonths;

    @Future(message = "Deadline must be in the future")
    private LocalDate deadline;

    private String imageUrl;
    private String fundingStage;
    private List<String> highlights;
}

