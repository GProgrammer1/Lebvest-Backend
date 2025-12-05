package com.lebvest.controller;

import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.UserRepository;
import com.lebvest.service.UserActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RestController
@RequestMapping("/api/user-activity")
public class UserActivityController {

    private final UserActivityService userActivityService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public UserActivityController(UserActivityService userActivityService, 
                                  SimpMessagingTemplate messagingTemplate,
                                  UserRepository userRepository) {
        this.userActivityService = userActivityService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /**
     * WebSocket endpoint for user heartbeat/ping
     * Clients send messages to /app/user/heartbeat
     */
    @MessageMapping("/user/heartbeat")
    public void handleHeartbeat(@Payload Map<String, Object> payload) {
        try {
            Long userId = getCurrentUserId();
            if (userId != null) {
                userActivityService.markUserOnline(userId);
            }
        } catch (Exception e) {
            log.error("Error handling heartbeat: {}", e.getMessage());
        }
    }

    /**
     * REST endpoint to get current online status of a user
     */
    @GetMapping("/status")
    public ResponseEntity<ResponsePayload> getUserStatus() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(
                        ResponsePayload.builder()
                                .status(401)
                                .message("Unauthorized")
                                .build()
                );
            }

            boolean isOnline = userActivityService.isUserOnline(userId);
            LocalDateTime lastActivity = userActivityService.getLastActivity(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("isOnline", isOnline);
            data.put("lastActivity", lastActivity);

            return ResponseEntity.ok(
                    ResponsePayload.builder()
                            .status(200)
                            .message("User status retrieved successfully")
                            .data(data)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error getting user status: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                    ResponsePayload.builder()
                            .status(500)
                            .message("Internal server error")
                            .build()
            );
        }
    }

    /**
     * Get current user ID from authentication context
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
                return null;
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> user = userRepository.findByEmail(userDetails.getUsername());
            return user.map(User::getId).orElse(null);
        } catch (Exception e) {
            log.error("Error extracting user ID from authentication: {}", e.getMessage());
            return null;
        }
    }
}

