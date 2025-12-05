package com.lebvest.controller;

import com.lebvest.model.dto.AdminNotificationDto;
import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.AdminNotificationType;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.AdminNotificationRepository;
import com.lebvest.repository.UserRepository;
import com.lebvest.service.JwtService;
import com.lebvest.util.AdminNotificationMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/sse/admin")
public class AdminNotificationSseController {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final AdminNotificationMapper adminNotificationMapper;
    private final JwtService jwtService;

    public AdminNotificationSseController(UserRepository userRepository,
                                          AdminNotificationRepository adminNotificationRepository,
                                          AdminNotificationMapper adminNotificationMapper,
                                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.adminNotificationMapper = adminNotificationMapper;
        this.jwtService = jwtService;
    }

    /**
     * Validates the token and user for SSE connection.
     * This method is transactional and read-only to ensure database connections are released quickly.
     */
    @Transactional(readOnly = true, timeout = 5)
    User validateAndGetUser(String token, Long adminId) {
        // Extract username from token
        String username = jwtService.extractClaim(token, "access", Claims::getSubject);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify user is admin
        if (!user.getRoles().contains(Role.ADMIN)) {
            log.warn("SSE connection attempt by non-admin user: {}", username);
            throw new IllegalArgumentException("User is not an admin");
        }
        
        // Verify adminId matches token's userId
        Long tokenUserId = jwtService.extractClaim(token, "access", claims -> {
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            }
            return null;
        });
        
        if (tokenUserId == null || !tokenUserId.equals(adminId)) {
            log.warn("SSE connection adminId mismatch. Token userId: {}, Requested adminId: {}", tokenUserId, adminId);
            throw new IllegalArgumentException("AdminId mismatch");
        }
        
        // Verify adminId matches user's actual ID
        if (!user.getId().equals(adminId)) {
            log.warn("SSE connection adminId mismatch. User ID: {}, Requested adminId: {}", user.getId(), adminId);
            throw new IllegalArgumentException("AdminId mismatch with user ID");
        }
        
        // Validate token - create UserDetails from User
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList()))
                .build();
        
        if (!jwtService.validateToken(token, "access", userDetails)) {
            log.warn("Invalid token for SSE connection: {}", username);
            throw new IllegalArgumentException("Invalid token");
        }
        
        return user;
    }
    
    @GetMapping(value = "/company-signups", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribeToSignupRequests(
            @RequestParam Long adminId,
            @RequestParam(required = false) String token) {
        
        // Validate token if provided (EventSource can't send headers, so token comes as query param)
        if (token == null || token.isEmpty()) {
            log.warn("SSE connection attempt without token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Validate user and token - this completes quickly and releases DB connection
        try {
            validateAndGetUser(token, adminId);
        } catch (Exception e) {
            log.error("Error validating token for SSE connection: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("not an admin")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout
        emitters.put(adminId, emitter);

        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE message: {}", e.getMessage());
        }

        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for admin: {}", adminId);
            emitters.remove(adminId);
        });
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for admin: {}", adminId);
            emitters.remove(adminId);
        });
        emitter.onError((ex) -> {
            log.error("SSE connection error for admin {}: {}", adminId, ex.getMessage());
            emitters.remove(adminId);
        });
        
        return ResponseEntity.ok(emitter);
    }


    @Async("taskExecutor")
    public void notifyAllAdmins(CompanySignupRequest request) {
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(Role.ADMIN))
                .toList();

        CompletableFuture.allOf(
                admins.stream().map(
                        (admin) -> CompletableFuture.supplyAsync(() -> {
                            AdminNotification notification = AdminNotification.builder()
                                    .admin(admin)
                                    .message("A new company signup request has been submitted by: " + request.getCompanyName())
                                    .type(AdminNotificationType.SIGNUP_REQUEST)
                                    .title("New company signup request")
                                    .isAccepted(null)
                                    .request(request)
                                    .build();

                            adminNotificationRepository.save(notification);
                            AdminNotificationDto dto = adminNotificationMapper.toDto(notification);
                            SseEmitter emitter = emitters.get(admin.getId());
                            if (emitter != null) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("company-signup")
                                            .data(dto));
                                } catch (IOException e) {
                                    log.error("Failed to notify admin {}: {}", admin.getId(), e.getMessage());
                                    emitters.remove(admin.getId());
                                }
                            }
                        return null;
                        })

                ).toArray(CompletableFuture[]::new)
        );

    }

    @Async("taskExecutor")
    public void notifyAllAdminsVerification(com.lebvest.model.entities.company.Company company) {
        try {
            log.info("Starting notification for company verification: {}", company.getName());
            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().contains(Role.ADMIN))
                    .toList();

            log.info("Found {} admin(s) to notify", admins.size());
            
            if (admins.isEmpty()) {
                log.warn("No admins found in database. Notification will not be sent.");
                return;
            }

            CompletableFuture.allOf(
                    admins.stream().map(
                            (admin) -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    log.info("Creating notification for admin: {} (ID: {})", admin.getEmail(), admin.getId());
                                    AdminNotification notification = AdminNotification.builder()
                                            .admin(admin)
                                            .message("Company " + company.getName() + " has submitted verification documents for review")
                                            .type(AdminNotificationType.VERIFICATION_REQUEST)
                                            .title("New verification documents submitted")
                                            .isAccepted(null)
                                            .company(company)
                                            .build();

                                    adminNotificationRepository.save(notification);
                                    log.info("Notification saved to database for admin: {} (ID: {})", admin.getEmail(), admin.getId());
                                    
                                    AdminNotificationDto dto = adminNotificationMapper.toDto(notification);
                                    SseEmitter emitter = emitters.get(admin.getId());
                                    if (emitter != null) {
                                        try {
                                            log.info("Sending SSE event to admin: {} (ID: {})", admin.getEmail(), admin.getId());
                                            emitter.send(SseEmitter.event()
                                                    .name("verification-request")
                                                    .data(dto));
                                            log.info("SSE event sent successfully to admin: {} (ID: {})", admin.getEmail(), admin.getId());
                                        } catch (IOException e) {
                                            log.error("Failed to send SSE event to admin {}: {}", admin.getId(), e.getMessage(), e);
                                            emitters.remove(admin.getId());
                                        }
                                    } else {
                                        log.warn("No SSE emitter found for admin: {} (ID: {}). Admin may not be connected. Notification saved to DB.", admin.getEmail(), admin.getId());
                                    }
                                } catch (Exception e) {
                                    log.error("Error creating notification for admin {}: {}", admin.getId(), e.getMessage(), e);
                                    e.printStackTrace();
                                }
                            return null;
                            })

                    ).toArray(CompletableFuture[]::new)
            ).thenRun(() -> {
                log.info("Completed notification process for company verification: {}", company.getName());
            }).exceptionally(ex -> {
                log.error("Error in notification process for company verification: {}", ex.getMessage(), ex);
                ex.printStackTrace();
                return null;
            });
        } catch (Exception e) {
            log.error("Critical error in notifyAllAdminsVerification: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Async("taskExecutor")
    public void notifyAllAdminsProject(com.lebvest.model.entities.investment.Investment investment) {
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(Role.ADMIN))
                .toList();

        CompletableFuture.allOf(
                admins.stream().map(
                        (admin) -> CompletableFuture.supplyAsync(() -> {
                            AdminNotification notification = AdminNotification.builder()
                                    .admin(admin)
                                    .message("A new project \"" + investment.getTitle() + "\" has been posted by " + investment.getCompany().getName())
                                    .type(AdminNotificationType.PROJECT_PROPOSAL)
                                    .title("New project proposal")
                                    .isAccepted(null)
                                    .investment(investment)
                                    .company(investment.getCompany())
                                    .build();

                            adminNotificationRepository.save(notification);
                            AdminNotificationDto dto = adminNotificationMapper.toDto(notification);
                            SseEmitter emitter = emitters.get(admin.getId());
                            if (emitter != null) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("project-proposal")
                                            .data(dto));
                                } catch (IOException e) {
                                    log.error("Failed to notify admin {}: {}", admin.getId(), e.getMessage());
                                    emitters.remove(admin.getId());
                                }
                            }
                        return null;
                        })

                ).toArray(CompletableFuture[]::new)
        );
    }

}
