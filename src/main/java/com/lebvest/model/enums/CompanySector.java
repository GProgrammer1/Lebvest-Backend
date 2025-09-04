package com.lebvest.model.enums;

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

    @Override
    public String toString() {
        return value;
    }
}
