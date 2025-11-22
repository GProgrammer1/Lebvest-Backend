package com.lebvest.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
public class ErrorPayload {
    private String message;
    private String path;
    private int status;
    private Map<String, String> errors;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
