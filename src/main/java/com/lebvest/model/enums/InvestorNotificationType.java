package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum InvestorNotificationType {
    NEW_OPPORTUNITY("New Opportunity"),
    UPDATE("Update"),
    THRESHOLD("Threshold"),
    NEWS("News"),
    INVESTMENT_ACCEPTED("Investment Accepted");

    private final String displayName;

    InvestorNotificationType(String displayName) {
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
}
