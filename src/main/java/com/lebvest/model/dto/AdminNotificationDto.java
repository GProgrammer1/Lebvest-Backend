package com.lebvest.model.dto;

import com.lebvest.model.enums.AdminNotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AdminNotificationDto {

    private Long id;
    private Long adminId;
    private Long reqId;
    private String title;
    private String message;
    private AdminNotificationType type;
    private Boolean isAccepted;
    private boolean isRead;
    public LocalDateTime createdAt;
}
