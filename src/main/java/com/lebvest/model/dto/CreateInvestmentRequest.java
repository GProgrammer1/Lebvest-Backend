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
public class CreateInvestmentRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
    private InvestmentCategory category;

    @NotNull(message = "Risk level is required")
    private RiskLevel riskLevel;

    @NotNull(message = "Expected return is required")
    @DecimalMin(value = "0.0", message = "Expected return must be positive")
    @DecimalMax(value = "100.0", message = "Expected return must not exceed 100%")
    private BigDecimal expectedReturn;

    @NotNull(message = "Minimum investment is required")
    @DecimalMin(value = "0.01", message = "Minimum investment must be positive")
    private BigDecimal minInvestment;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be positive")
    private BigDecimal targetAmount;

    @NotNull(message = "Location is required")
    private Location location;

    @NotNull(message = "Investment type is required")
    private InvestmentType investmentType;

    @NotNull(message = "Duration in months is required")
    @Min(value = 1, message = "Duration must be at least 1 month")
    private Integer durationMonths;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    private LocalDate deadline;

    private String imageUrl;
    private String fundingStage;
    private List<String> highlights;
}

