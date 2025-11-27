package com.lebvest.model.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoalRequest {
    private String title;
    
    @DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
    private BigDecimal targetAmount;
    
    private LocalDate deadline;
}

