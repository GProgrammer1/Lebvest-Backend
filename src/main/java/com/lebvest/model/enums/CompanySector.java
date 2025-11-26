package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum CompanySector {
    TECHNOLOGY("Technology"),
    FINANCE("Finance"),
    HEALTHCARE("Healthcare"),
    EDUCATION("Education"),
    ENERGY("Energy"),
    AGRICULTURE("Agriculture"),
    TRANSPORTATION("Transportation"),
    RETAIL("Retail"),
    REAL_ESTATE("Real Estate"),
    HOSPITALITY("Hospitality"),
    ENTERTAINMENT("Entertainment"),
    MANUFACTURING("Manufacturing");

    private final String value;

    CompanySector(String value) {
        this.value = value;
    }

    @JsonValue
    @Enumerated(EnumType.STRING)
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CompanySector fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Try to match by enum name first (case-insensitive)
        for (CompanySector sector : CompanySector.values()) {
            if (sector.name().equalsIgnoreCase(value)) {
                return sector;
            }
        }
        // Try to match by display name (case-insensitive)
        for (CompanySector sector : CompanySector.values()) {
            if (sector.value.equalsIgnoreCase(value)) {
                return sector;
            }
        }
        throw new IllegalArgumentException("Unknown CompanySector: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
