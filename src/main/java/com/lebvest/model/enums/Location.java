package com.lebvest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum Location {
    BEIRUT("Beirut"),
    MOUNT_LEBANON("Mount Lebanon"),
    NORTH("North"),
    SOUTH("South"),
    BEKAA("Bekaa"),
    NABATIEH("Nabatieh"),
    BAALBEK_HERMEL("Baalbek Hermel"),
    AKKAR("Akkar");

    private final String value;

    Location(String value) {
        this.value = value;
    }

    @JsonValue
    @Enumerated(EnumType.STRING)
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Location fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Try to match by enum name first (case-insensitive)
        for (Location location : Location.values()) {
            if (location.name().equalsIgnoreCase(value)) {
                return location;
            }
        }
        // Try to match by display name (case-insensitive)
        for (Location location : Location.values()) {
            if (location.value.equalsIgnoreCase(value)) {
                return location;
            }
        }
        throw new IllegalArgumentException("Unknown Location: " + value);
    }

    @Override public String toString() {
        return value;
    }
}