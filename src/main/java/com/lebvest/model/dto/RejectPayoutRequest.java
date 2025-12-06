package com.lebvest.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectPayoutRequest {
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
