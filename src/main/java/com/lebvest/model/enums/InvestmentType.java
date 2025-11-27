package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static InvestmentType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Try to match by enum name first (case-insensitive)
        for (InvestmentType type : InvestmentType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        // Try to match by display name (case-insensitive)
        for (InvestmentType type : InvestmentType.values()) {
            if (type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown InvestmentType: " + value);
    }
}
