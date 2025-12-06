package com.lebvest.model.dto;

import lombok.Data;

@Data
public class AcceptInvestmentRequestRequest {
    // Can add optional message/notes from company
    private String message;
}
