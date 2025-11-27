package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum InvestmentCategory {
    REAL_ESTATE("Real Estate"),
    GOVERNMENT_BONDS("Government Bonds"),
    STARTUP("Startup"),
    PERSONAL_PROJECT("Personal Project"),
    SME("Sme"),
    AGRICULTURE("Agriculture"),
    TECHNOLOGY("Technology"),
    EDUCATION("Education"),
    HEALTHCARE("Healthcare"),
    ENERGY("Energy"),
    TOURISM("Tourism"),
    RETAIL("Retail");

    private final String displayName;

    InvestmentCategory(String displayName) {
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
    public static InvestmentCategory fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Try to match by enum name first (case-insensitive)
        for (InvestmentCategory category : InvestmentCategory.values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        // Try to match by display name (case-insensitive)
        for (InvestmentCategory category : InvestmentCategory.values()) {
            if (category.displayName.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown InvestmentCategory: " + value);
    }
}
