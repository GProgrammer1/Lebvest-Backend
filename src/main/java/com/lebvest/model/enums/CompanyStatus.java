package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum CompanyStatus {
    PENDING("Pending"),
    APPROVED("Approved"), // Can browse, cannot post projects
    PENDING_DOCS("Pending Documentation"), // Step 2 documents submitted, awaiting admin approval
    FULLY_VERIFIED("Fully Verified"), // Can post projects
    REJECTED("Rejected");

    private final String displayName;

    CompanyStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    @Enumerated(EnumType.STRING)
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
    
    public boolean canPostProjects() {
        return this == FULLY_VERIFIED;
    }
}

