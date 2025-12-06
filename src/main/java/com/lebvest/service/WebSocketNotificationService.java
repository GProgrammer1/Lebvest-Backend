package com.lebvest.service;

import com.lebvest.model.entities.company.CompanyNotification;
import com.lebvest.repository.CompanyNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final CompanyNotificationRepository companyNotificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String NOTIFICATION_QUEUE_PREFIX = "notification:queue:";
    private static final int MESSAGE_TTL_HOURS = 24;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate,
                                       CompanyNotificationRepository companyNotificationRepository,
                                       RedisTemplate<String, Object> redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.companyNotificationRepository = companyNotificationRepository;
        this.redisTemplate = redisTemplate;
    }

    @Async("taskExecutor")
    @Transactional
    public void notifyCompany(Long companyId, CompanyNotification notification) {
        try {
            // Save notification to database
            companyNotificationRepository.save(notification);
            log.info("Notification saved to database for company: {} (ID: {}), Notification ID: {}", 
                    companyId, companyId, notification.getId());

            // Persist message in Redis for offline users
            String queueKey = NOTIFICATION_QUEUE_PREFIX + companyId;
            redisTemplate.opsForList().rightPush(queueKey, notification);
            redisTemplate.expire(queueKey, Duration.ofHours(MESSAGE_TTL_HOURS));

            // Send via WebSocket
            String destination = "/topic/company/" + companyId + "/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.info("WebSocket notification sent to company: {} (ID: {})", companyId, companyId);

        } catch (Exception e) {
            log.error("Error sending WebSocket notification to company {}: {}", 
                    companyId, e.getMessage(), e);
        }
    }

    @Async("taskExecutor")
    @Transactional
    public void notifyAdmin(Long adminId, Object notification) {
        try {
            // Persist message in Redis
            String queueKey = NOTIFICATION_QUEUE_PREFIX + "admin:" + adminId;
            redisTemplate.opsForList().rightPush(queueKey, notification);
            redisTemplate.expire(queueKey, Duration.ofHours(MESSAGE_TTL_HOURS));

            // Send via WebSocket
            String destination = "/topic/admin/" + adminId + "/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.info("WebSocket notification sent to admin: {}", adminId);

        } catch (Exception e) {
            log.error("Error sending WebSocket notification to admin {}: {}", 
                    adminId, e.getMessage(), e);
        }
    }

    /**
     * Retrieve pending notifications for a user when they reconnect
     */
    public java.util.List<Object> getPendingNotifications(Long userId, String userType) {
        String queueKey = NOTIFICATION_QUEUE_PREFIX + (userType.equals("admin") ? "admin:" : "") + userId;
        java.util.List<Object> notifications = redisTemplate.opsForList().range(queueKey, 0, -1);
        
        if (notifications != null && !notifications.isEmpty()) {
            // Clear the queue after retrieving
            redisTemplate.delete(queueKey);
            log.info("Retrieved {} pending notifications for {}: {}", 
                    notifications.size(), userType, userId);
        }
        
        return notifications != null ? notifications : java.util.Collections.emptyList();
    }

    /**
     * Send heartbeat to keep connection alive
     */
    public void sendHeartbeat(Long userId, String userType) {
        String destination = "/topic/" + (userType.equals("admin") ? "admin/" : "company/") + userId + "/heartbeat";
        messagingTemplate.convertAndSend(destination, "ping");
    }
}

