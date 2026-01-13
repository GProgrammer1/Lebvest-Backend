package com.lebvest.model.dto;

import com.lebvest.model.enums.AdminNotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AdminNotificationDto {

    private Long id;
    private Long adminId;
    private Long reqId;
    private Long companyId; // For verification requests
    private String title;
    private String message;
    private AdminNotificationType type;
    private Boolean isAccepted;
    private boolean read;
    public LocalDateTime createdAt;
    private List<String> documentUrls; // URLs to documents for viewing
}
