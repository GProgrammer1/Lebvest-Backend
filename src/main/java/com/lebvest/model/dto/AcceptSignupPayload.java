package com.lebvest.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class AcceptSignupPayload {

    @NotNull
    private Long reqId;

    @NotNull
    private Long notificationId;
}
