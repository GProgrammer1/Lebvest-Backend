package com.lebvest.model.enums;

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

    @Override public String toString() {
        return value;
    }
}