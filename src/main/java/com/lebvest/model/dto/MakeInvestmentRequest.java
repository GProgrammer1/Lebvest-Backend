package com.lebvest.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakeInvestmentRequest {
    @NotNull(message = "Investment amount is required")
    @DecimalMin(value = "0.01", message = "Investment amount must be greater than 0")
    private BigDecimal amount;
}

