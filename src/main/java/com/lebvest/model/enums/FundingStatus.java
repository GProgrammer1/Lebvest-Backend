package com.lebvest.model.enums;

public enum FundingStatus {
    PENDING,      // Investment request pending approval
    APPROVED,     // Approved, ready for payment
    PAID,         // Payment completed
    COMPLETED     // Funding goal reached, investment closed
}
