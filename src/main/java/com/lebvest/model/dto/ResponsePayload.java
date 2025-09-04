package com.lebvest.model.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ResponsePayload {


    private String message;
    private int status;
    private Map<String, ?> data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
