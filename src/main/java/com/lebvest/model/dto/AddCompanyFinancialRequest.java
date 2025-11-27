package com.lebvest.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCompanyFinancialRequest {
    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be valid")
    @Max(value = 2100, message = "Year must be valid")
    private Integer year;

    @NotNull(message = "Revenue is required")
    @DecimalMin(value = "0.0", message = "Revenue must be positive")
    private BigDecimal revenue;

    @NotNull(message = "Expenses is required")
    @DecimalMin(value = "0.0", message = "Expenses must be positive")
    private BigDecimal expenses;

    @NotNull(message = "Profit is required")
    private BigDecimal profit;
}

