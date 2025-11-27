package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum RiskLevel {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String value;

    RiskLevel(String value) {
        this.value = value;
    }

    @JsonValue
    @Enumerated(EnumType.STRING)
    public String getValue() {
        return value;
    }

    @Override public String toString() {
        return value;
    }

    @JsonCreator
    public static RiskLevel fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Try to match by enum name first (case-insensitive)
        for (RiskLevel level : RiskLevel.values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        // Try to match by display name (case-insensitive)
        for (RiskLevel level : RiskLevel.values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown RiskLevel: " + value);
    }
}