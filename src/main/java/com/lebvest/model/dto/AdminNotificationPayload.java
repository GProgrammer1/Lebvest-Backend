package com.lebvest.model.dto;

import com.lebvest.model.enums.AdminNotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationPayload {


    private String message;

    private AdminNotificationType type;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();


}
