package com.lebvest.model.enums;

public enum InvestmentStatus {
    PENDING_REVIEW,  // Submitted by company, awaiting admin review
    APPROVED,        // Approved by admin, visible to investors
    REJECTED,        // Rejected by admin
    DRAFT            // Company is still editing (optional, for future use)
}

