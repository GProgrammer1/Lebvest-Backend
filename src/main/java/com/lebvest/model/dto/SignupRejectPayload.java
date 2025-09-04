package com.lebvest.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class SignupRejectPayload {
    @NotNull
    private Long reqId;

    @NotBlank
    private String reason;

    @NotNull
    private Long notificationId;
}
