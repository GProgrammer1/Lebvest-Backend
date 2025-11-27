package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.model.dto.AcceptSignupPayload;
import com.lebvest.model.dto.AdminNotificationDto;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.SignupRejectPayload;
import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.model.enums.SignupRequestStatus;
import com.lebvest.repository.AdminNotificationRepository;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.CompanySignupRequestRepository;
import com.lebvest.repository.UserRepository;
import com.lebvest.util.AdminNotificationMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;  // Disabled - RabbitMQ not needed
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminService {

    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final VarsConfig varsConfig;
    private final S3AsyncClient s3Async;
    private final CleanupService cleanupService;
    private final AdminNotificationRepository adminNotificationRepository;
    private final CompanySignupRequestRepository companySignupRequestRepository;
    //private final RabbitTemplate rabbitTemplate;  // Disabled - RabbitMQ not needed

    public AdminService(UserRepository userRepo,
                        CompanyRepository companyRepo,
                        PasswordEncoder passwordEncoder,
                        S3Service s3Service,
                        VarsConfig varsConfig,
                        S3AsyncClient s3Async,
                        CleanupService cleanupService,
                        AdminNotificationRepository adminNotificationRepository,
                        CompanySignupRequestRepository companySignupRequestRepository
                        //RabbitTemplate rabbitTemplate  // Disabled - RabbitMQ not needed
                        ) {
        this.userRepo = userRepo;
        this.companyRepo = companyRepo;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
        this.varsConfig = varsConfig;
        this.s3Async = s3Async;
        this.cleanupService = cleanupService;
        this.adminNotificationRepository = adminNotificationRepository;
        this.companySignupRequestRepository = companySignupRequestRepository;
        //this.rabbitTemplate = rabbitTemplate;  // Disabled - RabbitMQ not needed
    }

    @Transactional
    public ResponsePayload acceptSignupRequest(AcceptSignupPayload payload) {
        Long id = payload.getReqId();

        // 1) Update state
        CompanySignupRequest request = updateRequestStatus(id, SignupRequestStatus.ACCEPTED);
        updateAdminNotification(payload.getNotificationId(), true);

        // 2) Create user
        User user = buildUser(request);

        // 3) Persist Company with accepted paths (map pending -> accepted)
        List<String> acceptedKeys = Optional.ofNullable(request.getDocuments())
                .orElseGet(List::of)
                .stream()
                .map(pendingKey -> {
                    String pendingPrefix = varsConfig.getPendingPrefix(request.getRequestId()) + "/";
                    String acceptedPrefix = varsConfig.getAcceptedPrefix(request.getRequestId()) + "/";
                    if (pendingKey != null && pendingKey.startsWith(pendingPrefix)) {
                        return acceptedPrefix + pendingKey.substring(pendingPrefix.length());
                    }
                    String fileName = pendingKey == null ? "" :
                            (pendingKey.contains("/") ? pendingKey.substring(pendingKey.lastIndexOf('/') + 1) : pendingKey);
                    return acceptedPrefix + fileName;
                })
                .collect(Collectors.toList());

        Company company = buildCompany(request, user, acceptedKeys);

        // 4) Queue the S3 move (pending -> accepted)
        String moveQueue = resolveQueueName(varsConfig.getSignupCompanyAcceptedMoveQueueName(), "company.signup.accepted.move");
        var moveEvent = new com.lebvest.model.events.CompanySignupAcceptedMoveEvent(
                request.getRequestId(),
                request.getDocuments() // optional: exact pending keys
        );
        //rabbitTemplate.convertAndSend(moveQueue, moveEvent);  // Disabled - RabbitMQ not needed

        // 5) Queue the accepted email (Admin -> Company)
        String emailQueue = resolveQueueName(varsConfig.getSignupCompanyEmailQueueName(), "company.signup.email");
        Map<String, String> templateData = Map.of(
                "name", user.getName(),
                "companyName", company.getName(),
                "loginUrl", varsConfig.getFrontendUrl() + "/signin",
                "sector", company.getSector() != null ? company.getSector().toString() : "N/A",
                "email", user.getEmail()
        );
        var emailEvent = new com.lebvest.model.events.CompanySignupEmailEvent(
                "LebVest Account Creation Approved",
                "CompanySignupSuccess",
                templateData,
                null,
                user.getEmail()
        );
        //rabbitTemplate.convertAndSend(emailQueue, emailEvent);  // Disabled - RabbitMQ not needed

        return ResponsePayload.builder()
                .message("Company signup accepted successfully")
                .status(200)
                .build();
    }

    public ResponsePayload rejectRequest(SignupRejectPayload payload) {
        Long id = payload.getReqId();
        String reason = payload.getReason();

        CompanySignupRequest req = updateRequestStatus(id, SignupRequestStatus.REJECTED);
        updateAdminNotification(payload.getNotificationId(), false);

        String prefix = varsConfig.getPendingPrefix(req.getRequestId());
        CompletableFuture.runAsync(() -> s3Service.deleteFolderByPrefix(prefix))
                .exceptionally((err) -> {
                    log.error("Error rejecting request : {}", err.getMessage());
                    return null;
                });

        // Queue the rejection email (Admin -> Company)
        String emailQueue = resolveQueueName(varsConfig.getSignupCompanyEmailQueueName(), "company.signup.email");
        Map<String, String> templateData = Map.of(
                "name", req.getName(),
                "companyName", req.getCompanyName(),
                "reason", reason,
                "sector", req.getSector() != null ? req.getSector().getValue() : "sector",
                "email", req.getEmail()
        );
        var emailEvent = new com.lebvest.model.events.CompanySignupEmailEvent(
                "Company signup rejected",
                "CompanySignupFailure",
                templateData,
                null,
                req.getEmail()
        );
        //rabbitTemplate.convertAndSend(emailQueue, emailEvent);  // Disabled - RabbitMQ not needed

        return ResponsePayload.builder()
                .message("Request to signup rejected")
                .status(200)
                .build();
    }

    private String resolveQueueName(String fromConfig, String fallback) {
        return (fromConfig != null && !fromConfig.isBlank()) ? fromConfig : fallback;
    }

    private void updateAdminNotification(Long notificationId, boolean accepted) {
        AdminNotification notification = adminNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin notification id: " + notificationId));
        notification.setIsAccepted(accepted);
        adminNotificationRepository.save(notification);
    }

    private User buildUser(CompanySignupRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();
        Set<Role> newRoles = new HashSet<>(user.getRoles());
        newRoles.add(Role.COMPANY);
        user.setRoles(newRoles);
        userRepo.save(user);
        return user;
    }

    private Company buildCompany(CompanySignupRequest request, User user, List<String> acceptedKeys) {
        Company company = Company.builder()
                .name(request.getCompanyName())
                .documents(acceptedKeys)
                .description(request.getDescription())
                .logo(request.getLogo())
                .sector(request.getSector())
                .location(request.getLocation())
                .foundedYear(request.getFoundedYear())
                .user(user)
                .build();
        companyRepo.save(company);
        return company;
    }

    public ResponsePayload getAllNotifications() {
        List<AdminNotificationDto> notifications = adminNotificationRepository.findAll()
                .stream().map(AdminNotificationMapper::toDto).toList();
        return ResponsePayload.builder()
                .message("Notifications retrieved successfully")
                .status(200)
                .data(Map.of("notifications", notifications))
                .build();
    }

    public CompanySignupRequest updateRequestStatus(Long id, SignupRequestStatus status) {
        CompanySignupRequest req = companySignupRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid signup request id: " + id));
        req.setRequestStatus(status);
        companySignupRequestRepository.save(req);
        return req;
    }

    public ResponsePayload readNotification(Long id) {
        AdminNotification notification = adminNotificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin notification id: " + id));
        notification.setRead(true);
        adminNotificationRepository.save(notification);
        var notificationDto = AdminNotificationMapper.toDto(notification);

        return ResponsePayload.builder()
                .message("Notification marked as read")
                .status(200)
                .data(Map.of("notification", notificationDto))
                .build();
    }
}
