package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum InvestmentType {
    EQUITY("Equity"),
    DEBT("Debt"),
    CROWDFUNDING("Crowdfunding");

    private final String displayName;

    InvestmentType(String displayName) {
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
