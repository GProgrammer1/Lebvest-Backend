package com.lebvest.util;

import com.lebvest.model.dto.AdminNotificationDto;
import com.lebvest.model.entities.admin.AdminNotification;
import org.springframework.stereotype.Component;

@Component
public class AdminNotificationMapper implements GenericMapper<AdminNotification, AdminNotificationDto>{

    public AdminNotification toEntity(AdminNotificationDto adminNotificationDto) {
        return AdminNotification.builder()
                .id(adminNotificationDto.getId())
                .type(adminNotificationDto.getType())
                .title(adminNotificationDto.getTitle())
                .message(adminNotificationDto.getMessage())
                .isAccepted(adminNotificationDto.getIsAccepted())
                .isRead(adminNotificationDto.isRead())
                .createdAt(adminNotificationDto.getCreatedAt())
                .build();
    }

    public static AdminNotificationDto toDto(AdminNotification adminNotification) {
        return AdminNotificationDto
                .builder()
                .id(adminNotification.getId())
                .adminId(adminNotification.getAdmin().getId())
                .reqId(adminNotification.getRequest().getId())
                .title(adminNotification.getTitle())
                .message(adminNotification.getMessage())
                .isAccepted(adminNotification.getIsAccepted())
                .isRead(adminNotification.isRead())
                .type(adminNotification.getType())
                .createdAt(adminNotification.getCreatedAt())
                .build();
    }
}
