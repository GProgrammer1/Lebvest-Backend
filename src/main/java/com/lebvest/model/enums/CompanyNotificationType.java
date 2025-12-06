package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CompanyNotificationType {
    INVESTOR_INQUIRY("Investor Inquiry"),
    INVESTOR_REQUEST("Investor Request"),
    FUNDING_MILESTONE("Funding Milestone"),
    ADMIN_MESSAGE("Admin Message"),
    PROJECT_APPROVED("Project Approved"),
    PROJECT_REJECTED("Project Rejected");

    private final String displayName;

    CompanyNotificationType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static CompanyNotificationType fromDisplayName(String displayName) {
        for (CompanyNotificationType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CompanyNotificationType: " + displayName);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
