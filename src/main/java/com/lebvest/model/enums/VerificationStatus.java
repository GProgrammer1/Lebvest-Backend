package com.lebvest.model.enums;

public enum VerificationStatus {
    PENDING,              // Document submitted, awaiting review
    APPROVED,             // Document approved
    REJECTED,             // Document rejected
    RESUBMIT_REQUIRED     // Company needs to resubmit
}
