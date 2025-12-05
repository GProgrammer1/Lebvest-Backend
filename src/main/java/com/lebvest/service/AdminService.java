package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.model.dto.AcceptSignupPayload;
import com.lebvest.model.dto.AdminNotificationDto;
import com.lebvest.model.dto.AdminStatisticsDto;
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
import com.lebvest.repository.CompanyVerificationDocumentsRepository;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import com.lebvest.model.entities.company.CompanyVerificationDocuments;
import com.lebvest.model.enums.CompanyStatus;
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
    private final InvestmentRepository investmentRepository;
    private final InvestorRepository investorRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final LocalFileStorageService localFileStorageService;
    private final CompanyVerificationDocumentsRepository verificationDocumentsRepository;
    private final MailService mailService;
    //private final RabbitTemplate rabbitTemplate;  // Disabled - RabbitMQ not needed

    public AdminService(UserRepository userRepo,
                        CompanyRepository companyRepo,
                        PasswordEncoder passwordEncoder,
                        S3Service s3Service,
                        VarsConfig varsConfig,
                        S3AsyncClient s3Async,
                        CleanupService cleanupService,
                        AdminNotificationRepository adminNotificationRepository,
                        CompanySignupRequestRepository companySignupRequestRepository,
                        InvestmentRepository investmentRepository,
                        InvestorRepository investorRepository,
                        InvestorInvestmentRepository investorInvestmentRepository,
                        LocalFileStorageService localFileStorageService,
                        CompanyVerificationDocumentsRepository verificationDocumentsRepository,
                        MailService mailService
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
        this.investmentRepository = investmentRepository;
        this.investorRepository = investorRepository;
        this.investorInvestmentRepository = investorInvestmentRepository;
        this.localFileStorageService = localFileStorageService;
        this.verificationDocumentsRepository = verificationDocumentsRepository;
        this.mailService = mailService;
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

        // 3) Move files from pending to accepted using LocalFileStorageService
        List<String> acceptedKeys = localFileStorageService.moveToAccepted(
                request.getRequestId(), 
                Optional.ofNullable(request.getDocuments()).orElseGet(List::of)
        );

        Company company = buildCompany(request, user, acceptedKeys);

        // 4) Queue the S3 move (pending -> accepted)
        String moveQueue = resolveQueueName(varsConfig.getSignupCompanyAcceptedMoveQueueName(), "company.signup.accepted.move");
        var moveEvent = new com.lebvest.model.events.CompanySignupAcceptedMoveEvent(
                request.getRequestId(),
                request.getDocuments() // optional: exact pending keys
        );
        //rabbitTemplate.convertAndSend(moveQueue, moveEvent);  // Disabled - RabbitMQ not needed

        // 5) Send the accepted email (Admin -> Company) - Stage 1 approval
        // Use custom sector if sector is OTHER, otherwise use the sector display name
        String sectorDisplay = company.getSector() != null 
            ? (company.getSector() == com.lebvest.model.enums.CompanySector.OTHER && request.getCustomSector() != null && !request.getCustomSector().trim().isEmpty()
                ? request.getCustomSector() 
                : company.getSector().toString())
            : "N/A";
        Map<String, String> templateData = new HashMap<>();
        templateData.put("name", user.getName());
        templateData.put("companyName", company.getName());
        templateData.put("loginUrl", varsConfig.getFrontendUrl() + "/signin");
        templateData.put("verificationUrl", varsConfig.getFrontendUrl() + "/company-verification");
        templateData.put("sector", sectorDisplay);
        templateData.put("email", user.getEmail());
        
        String htmlContent = mailService.loadAndFormatEmailTemplate(templateData, "CompanySignupSuccess");
        mailService.sendHtmlMail(user.getEmail(), "LebVest Account Creation Approved", htmlContent);

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

        // Delete pending files using LocalFileStorageService
        CompletableFuture.runAsync(() -> localFileStorageService.deletePendingFiles(req.getRequestId()))
                .exceptionally((err) -> {
                    log.error("Error deleting pending files: {}", err.getMessage());
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
        // Password is already encoded in CompanySignupRequest, use it directly
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword()) // Already encoded
                .enabled(true)
                .build();
        // Initialize roles if null, then add COMPANY role
        Set<Role> roles = user.getRoles();
        if (roles == null) {
            roles = new HashSet<>();
        } else {
            roles = new HashSet<>(roles);
        }
        roles.add(Role.COMPANY);
        user.setRoles(roles);
        userRepo.save(user);
        return user;
    }

    private Company buildCompany(CompanySignupRequest request, User user, List<String> acceptedKeys) {
        // Build location string from governorate and city
        String governorate = request.getGovernorate() != null ? request.getGovernorate() : "";
        String city = request.getCity() != null ? request.getCity() : "";
        String location = (governorate.isEmpty() && city.isEmpty()) ? "N/A" : governorate + ", " + city;
        
        Company company = Company.builder()
                .name(request.getCompanyName())
                .documents(acceptedKeys)
                .logo(request.getLogo())
                .sector(request.getSector())
                .customSector(request.getCustomSector())
                .location(location)
                .governorate(request.getGovernorate())
                .city(request.getCity())
                .phoneNumber(request.getPhoneNumber())
                .website(request.getWebsite())
                .foundedYear(request.getFoundedYear())
                .user(user)
                .status(com.lebvest.model.enums.CompanyStatus.APPROVED) // Can browse, but needs step 2 for posting
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminStatisticsDto getStatistics() {
        // Count companies
        long totalCompanies = companyRepo.count();
        
        // Count investors
        long totalInvestors = investorRepository.count();
        
        // Count investments
        long totalInvestments = investmentRepository.count();
        
        // Count active investments (deadline in future)
        long activeInvestments = investmentRepository.findAll().stream()
                .filter(inv -> inv.getDeadline() != null && 
                        inv.getDeadline().isAfter(java.time.LocalDate.now()))
                .count();
        
        // Calculate total raised and target amounts
        java.math.BigDecimal totalRaisedAmount = investmentRepository.findAll().stream()
                .map(inv -> inv.getRaisedAmount() != null ? inv.getRaisedAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal totalTargetAmount = investmentRepository.findAll().stream()
                .map(inv -> inv.getTargetAmount() != null ? inv.getTargetAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        // Count total investor investments
        long totalInvestorInvestments = investorInvestmentRepository.count();
        
        // Count pending signup requests
        long pendingSignupRequests = companySignupRequestRepository.findAll().stream()
                .filter(req -> req.getRequestStatus() == SignupRequestStatus.PENDING)
                .count();
        
        return AdminStatisticsDto.builder()
                .totalCompanies(totalCompanies)
                .totalInvestors(totalInvestors)
                .totalInvestments(totalInvestments)
                .activeInvestments(activeInvestments)
                .totalRaisedAmount(totalRaisedAmount)
                .totalTargetAmount(totalTargetAmount)
                .totalInvestorInvestments(totalInvestorInvestments)
                .pendingSignupRequests(pendingSignupRequests)
                .build();
    }

    @Transactional
    public ResponsePayload approveVerificationDocuments(Long companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (company.getStatus() != CompanyStatus.PENDING_DOCS) {
            throw new IllegalStateException("Company is not in PENDING_DOCS status");
        }

        CompanyVerificationDocuments docs = verificationDocumentsRepository.findByCompany(company)
                .orElseThrow(() -> new IllegalArgumentException("Verification documents not found"));

        // Approve documents
        docs.setIsApproved(true);
        verificationDocumentsRepository.save(docs);

        // Update company status to FULLY_VERIFIED
        company.setStatus(CompanyStatus.FULLY_VERIFIED);
        companyRepo.save(company);

        // Send email to company
        sendVerificationApprovalEmail(company);

        log.info("Verification documents approved for company: {}", company.getName());

        return ResponsePayload.builder()
                .status(200)
                .message("Company verification approved. Company can now post projects.")
                .data(Map.of("companyId", companyId, "status", "FULLY_VERIFIED"))
                .build();
    }

    private void sendVerificationApprovalEmail(Company company) {
        try {
            String companyEmail = company.getUser().getEmail();
            String loginUrl = varsConfig.getFrontendUrl() + "/signin";

            Map<String, String> templateData = new HashMap<>();
            templateData.put("name", company.getUser().getName());
            templateData.put("companyName", company.getName());
            templateData.put("loginUrl", loginUrl);

            String htmlContent = mailService.loadAndFormatEmailTemplate(templateData, "CompanyVerificationApproval");
            mailService.sendHtmlMail(companyEmail, "Verification Approved - You Can Now Post Projects", htmlContent);
            log.info("Verification approval email sent to: {}", companyEmail);
        } catch (Exception e) {
            log.error("Failed to send verification approval email: {}", e.getMessage(), e);
        }
    }
}
