package com.lebvest.model.enums;

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
}