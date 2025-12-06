package com.lebvest.model.enums;

public enum PayoutStatus {
    PENDING,           // Payout request created, awaiting company action
    SUBMITTED,         // Company submitted payout evidence
    VERIFYING,         // Admin is verifying payout
    APPROVED,          // Admin approved, payout processed
    REJECTED,          // Admin rejected payout
    COMPLETED          // Payout successfully completed
}
