package com.lebvest.model.enums;

import lombok.Getter;

@Getter
public enum AdminNotificationType {
    SIGNUP_REQUEST("Signup Request"),
    PROJECT_PROPOSAL("Project Proposal"),
    APP_STAT_UPDATE("App Stats Update");

    private final String value;

    AdminNotificationType(String value) {
        this.value = value;
    }


}
