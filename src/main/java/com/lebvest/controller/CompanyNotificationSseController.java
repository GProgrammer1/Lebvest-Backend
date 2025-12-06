package com.lebvest.controller;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanyNotification;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.CompanyNotificationType;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.CompanyNotificationRepository;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.UserRepository;
import com.lebvest.service.JwtService;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/sse/company")
public class CompanyNotificationSseController {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyNotificationRepository companyNotificationRepository;
    private final InvestmentRepository investmentRepository;
    private final JwtService jwtService;

    public CompanyNotificationSseController(UserRepository userRepository,
                                          CompanyRepository companyRepository,
                                          CompanyNotificationRepository companyNotificationRepository,
                                          InvestmentRepository investmentRepository,
                                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyNotificationRepository = companyNotificationRepository;
        this.investmentRepository = investmentRepository;
        this.jwtService = jwtService;
    }

    /**
     * Validates the token and company for SSE connection.
     */
    @Transactional(readOnly = true, timeout = 5)
    Company validateAndGetCompany(String token, Long companyId) {
        // Extract username from token
        String username = jwtService.extractClaim(token, "access", Claims::getSubject);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify user is a company
        if (!user.getRoles().contains(Role.COMPANY)) {
            log.warn("SSE connection attempt by non-company user: {}", username);
            throw new IllegalArgumentException("User is not a company");
        }
        
        // Get company
        Company company = companyRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Company not found for user"));
        
        // Verify companyId matches
        if (!company.getId().equals(companyId)) {
            log.warn("SSE connection companyId mismatch. User company ID: {}, Requested companyId: {}", company.getId(), companyId);
            throw new IllegalArgumentException("CompanyId mismatch");
        }
        
        // Validate token
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
        
        return company;
    }
    
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribeToNotifications(
            @RequestParam Long companyId,
            @RequestParam(required = false) String token) {
        
        if (token == null || token.isEmpty()) {
            log.warn("SSE connection attempt without token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            validateAndGetCompany(token, companyId);
        } catch (Exception e) {
            log.error("Error validating token for SSE connection: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("not a company")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout
        emitters.put(companyId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE message: {}", e.getMessage());
        }

        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for company: {}", companyId);
            emitters.remove(companyId);
        });
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for company: {}", companyId);
            emitters.remove(companyId);
        });
        emitter.onError((ex) -> {
            log.error("SSE connection error for company {}: {}", companyId, ex.getMessage());
            emitters.remove(companyId);
        });
        
        return ResponseEntity.ok(emitter);
    }

    @Async("taskExecutor")
    @Transactional
    public void notifyCompany(Long companyId, CompanyNotificationType type, String title, String message, Long investmentId) {
        try {
            // Fetch company and investment in new transaction context to avoid detached entity issues
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
            
            Investment relatedInvestment = null;
            // TODO: After running initialize_investment_version.sql migration, uncomment below to set investment reference
            // For now, setting to null to avoid version field issues with existing records
            // The investment ID is already included in the notification message, so functionality is not affected
            /*
            if (investmentId != null) {
                try {
                    // After migration, version will be initialized and this will work
                    relatedInvestment = investmentRepository.getReferenceById(investmentId);
                } catch (Exception e) {
                    log.warn("Could not load investment reference for ID {}: {}", investmentId, e.getMessage());
                }
            }
            */
            
            log.info("Sending SSE notification to company: {} (ID: {})", company.getName(), company.getId());
            
            // Create notification with managed entities
            CompanyNotification notification = CompanyNotification.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .company(company)
                    .type(type)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .relatedInvestment(relatedInvestment)
                    .build();
            
            // Save notification to database
            companyNotificationRepository.save(notification);
            log.info("Notification saved to database for company: {} (ID: {}), Notification ID: {}", 
                    company.getName(), company.getId(), notification.getId());
            
            // Send via SSE
            SseEmitter emitter = emitters.get(company.getId());
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(notification));
                    log.info("SSE notification sent successfully to company: {} (ID: {})", 
                            company.getName(), company.getId());
                } catch (IOException e) {
                    log.error("Failed to send SSE notification to company {} (ID: {}): {}", 
                            company.getId(), company.getName(), e.getMessage());
                    emitters.remove(company.getId());
                }
            } else {
                log.warn("No SSE emitter found for company: {} (ID: {}). Notification saved to DB.", 
                        company.getName(), company.getId());
            }
        } catch (Exception e) {
            log.error("Error sending notification to company {}: {}", 
                    companyId, e.getMessage(), e);
        }
    }
}
