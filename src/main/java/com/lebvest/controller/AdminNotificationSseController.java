package com.lebvest.controller;

import com.lebvest.model.dto.AdminNotificationDto;
import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.AdminNotificationType;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.AdminNotificationRepository;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.UserRepository;
import com.lebvest.util.AdminNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/sse/admin")
public class AdminNotificationSseController {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final AdminNotificationMapper adminNotificationMapper;

    public AdminNotificationSseController(UserRepository userRepository,
                                          AdminNotificationRepository adminNotificationRepository,
                                          AdminNotificationMapper adminNotificationMapper) {
        this.userRepository = userRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.adminNotificationMapper = adminNotificationMapper;
    }

    @GetMapping(value = "/company-signups", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToSignupRequests(@RequestParam Long adminId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(adminId, emitter);

        emitter.onTimeout(() -> emitters.remove(adminId));
        emitter.onCompletion(() -> emitters.remove(adminId));
        return emitter;
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
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(Role.ADMIN))
                .toList();

        CompletableFuture.allOf(
                admins.stream().map(
                        (admin) -> CompletableFuture.supplyAsync(() -> {
                            AdminNotification notification = AdminNotification.builder()
                                    .admin(admin)
                                    .message("Company " + company.getName() + " has submitted verification documents for review")
                                    .type(AdminNotificationType.VERIFICATION_REQUEST)
                                    .title("New verification documents submitted")
                                    .isAccepted(null)
                                    .company(company)
                                    .build();

                            adminNotificationRepository.save(notification);
                            AdminNotificationDto dto = adminNotificationMapper.toDto(notification);
                            SseEmitter emitter = emitters.get(admin.getId());
                            if (emitter != null) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("verification-request")
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
