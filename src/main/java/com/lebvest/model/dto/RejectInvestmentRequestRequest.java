package com.lebvest.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectInvestmentRequestRequest {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
    
    private String message; // Optional additional message
}
