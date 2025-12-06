package com.lebvest.model.enums;

public enum InvestmentRequestStatus {
    PENDING,    // Request sent, awaiting company response
    ACCEPTED,   // Company accepted, investor can pay
    REJECTED,   // Company rejected
    PAID,       // Payment completed
    CANCELLED  // Request cancelled (by investor or system)
}
