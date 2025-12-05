package com.lebvest.service;

import com.lebvest.model.dto.UserActivityDto;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserActivityService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Track online users: userId -> lastActivity timestamp
    private final Map<Long, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();
    
    // Timeout for considering user offline (5 minutes)
    private static final int OFFLINE_TIMEOUT_MINUTES = 5;

    public UserActivityService(UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Mark user as active/online
     */
    public void markUserOnline(Long userId) {
        if (userId == null) return;
        
        LocalDateTime now = LocalDateTime.now();
        boolean wasOnline = onlineUsers.containsKey(userId);
        onlineUsers.put(userId, now);
        
        // If user just came online, notify admins
        if (!wasOnline) {
            notifyUserStatusChange(userId, true);
            log.debug("User {} marked as online", userId);
        }
    }

    /**
     * Mark user as offline
     */
    public void markUserOffline(Long userId) {
        if (userId == null) return;
        
        if (onlineUsers.remove(userId) != null) {
            notifyUserStatusChange(userId, false);
            log.debug("User {} marked as offline", userId);
        }
    }

    /**
     * Check if user is currently online
     */
    public boolean isUserOnline(Long userId) {
        if (userId == null) return false;
        LocalDateTime lastActivity = onlineUsers.get(userId);
        if (lastActivity == null) return false;
        
        // Check if last activity is within timeout period
        return lastActivity.isAfter(LocalDateTime.now().minusMinutes(OFFLINE_TIMEOUT_MINUTES));
    }

    /**
     * Get user's last activity timestamp
     */
    public LocalDateTime getLastActivity(Long userId) {
        return onlineUsers.get(userId);
    }

    /**
     * Notify all admins about user status change
     */
    private void notifyUserStatusChange(Long userId, boolean isOnline) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            UserActivityDto activity = UserActivityDto.builder()
                    .userId(userId)
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .isOnline(isOnline)
                    .lastSeen(isOnline ? LocalDateTime.now() : getLastActivity(userId))
                    .lastActivity(getLastActivity(userId))
                    .build();

            // Send to all admins subscribed to /topic/user-activity
            messagingTemplate.convertAndSend("/topic/user-activity", activity);
            log.debug("Notified admins about user {} status: {}", userId, isOnline ? "online" : "offline");
        } catch (Exception e) {
            log.error("Error notifying user status change for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Scheduled task to check for inactive users and mark them as offline
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void checkInactiveUsers() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(OFFLINE_TIMEOUT_MINUTES);
        
        onlineUsers.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(timeoutThreshold)) {
                Long userId = entry.getKey();
                log.debug("User {} timed out, marking as offline", userId);
                notifyUserStatusChange(userId, false);
                return true;
            }
            return false;
        });
    }

    /**
     * Get all currently online users
     */
    public Map<Long, LocalDateTime> getOnlineUsers() {
        return new ConcurrentHashMap<>(onlineUsers);
    }
}

