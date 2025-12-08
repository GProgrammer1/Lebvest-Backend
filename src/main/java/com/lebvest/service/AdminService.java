package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.model.dto.*;
import com.lebvest.model.dto.AdminProjectReviewDto;
import com.lebvest.model.dto.ApproveProjectRequest;
import com.lebvest.model.dto.RejectProjectRequest;
import com.lebvest.model.dto.UserDto;
import com.lebvest.model.dto.UpdateUserStatusRequest;
import com.lebvest.model.dto.WatchlistStatusDto;
import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanyNotification;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.enums.CompanyNotificationType;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentStatus;
import com.lebvest.model.enums.Role;
import com.lebvest.model.enums.SignupRequestStatus;
import com.lebvest.repository.AdminNotificationRepository;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.CompanySignupRequestRepository;
import com.lebvest.repository.CompanyVerificationDocumentsRepository;
import com.lebvest.repository.CompanyNotificationRepository;
import com.lebvest.repository.VerificationDocumentHistoryRepository;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestmentHighlightRepository;
import com.lebvest.repository.InvestmentFinancialRepository;
import com.lebvest.repository.InvestmentDocumentRepository;
import com.lebvest.repository.InvestmentTeamMemberRepository;
import com.lebvest.repository.InvestmentUpdateRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.lebvest.model.entities.company.CompanyVerificationDocuments;
import com.lebvest.model.enums.CompanyStatus;
import com.lebvest.service.UserActivityService;
import com.lebvest.util.AdminNotificationMapper;
import com.lebvest.controller.CompanyNotificationSseController;
import jakarta.transaction.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;  // Disabled - RabbitMQ not needed
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final InvestmentHighlightRepository investmentHighlightRepository;
    private final InvestmentFinancialRepository investmentFinancialRepository;
    private final InvestmentDocumentRepository investmentDocumentRepository;
    private final InvestmentTeamMemberRepository investmentTeamMemberRepository;
    private final InvestmentUpdateRepository investmentUpdateRepository;
    private final InvestorRepository investorRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final LocalFileStorageService localFileStorageService;
    private final CompanyVerificationDocumentsRepository verificationDocumentsRepository;
    private final MailService mailService;
    private final UserActivityService userActivityService;
    private final AdminNotificationMapper adminNotificationMapper;
    private final CompanyNotificationRepository companyNotificationRepository;
    private final CompanyNotificationSseController companyNotificationSseController;
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
                        InvestmentHighlightRepository investmentHighlightRepository,
                        InvestmentFinancialRepository investmentFinancialRepository,
                        InvestmentDocumentRepository investmentDocumentRepository,
                        InvestmentTeamMemberRepository investmentTeamMemberRepository,
                        InvestmentUpdateRepository investmentUpdateRepository,
                        InvestorRepository investorRepository,
                        InvestorInvestmentRepository investorInvestmentRepository,
                        LocalFileStorageService localFileStorageService,
                        CompanyVerificationDocumentsRepository verificationDocumentsRepository,
                        MailService mailService,
                        UserActivityService userActivityService,
                        AdminNotificationMapper adminNotificationMapper,
                        CompanyNotificationRepository companyNotificationRepository,
                        CompanyNotificationSseController companyNotificationSseController
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
        this.investmentHighlightRepository = investmentHighlightRepository;
        this.investmentFinancialRepository = investmentFinancialRepository;
        this.investmentDocumentRepository = investmentDocumentRepository;
        this.investmentTeamMemberRepository = investmentTeamMemberRepository;
        this.investmentUpdateRepository = investmentUpdateRepository;
        this.investorRepository = investorRepository;
        this.investorInvestmentRepository = investorInvestmentRepository;
        this.localFileStorageService = localFileStorageService;
        this.verificationDocumentsRepository = verificationDocumentsRepository;
        this.mailService = mailService;
        this.userActivityService = userActivityService;
        this.adminNotificationMapper = adminNotificationMapper;
        this.companyNotificationRepository = companyNotificationRepository;
        this.companyNotificationSseController = companyNotificationSseController;
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
        // Get current admin user from security context
        org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated request to get notifications");
            return ResponsePayload.builder()
                    .message("Unauthorized")
                    .status(401)
                    .data(Map.of("notifications", new ArrayList<>()))
                    .build();
        }
        
        String email = authentication.getName();
        User currentAdmin = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
        
        log.info("Fetching notifications for admin: {} (ID: {})", email, currentAdmin.getId());
        
        // Filter notifications by current admin
        List<AdminNotificationDto> notifications = adminNotificationRepository.findAll()
                .stream()
                .filter(notification -> notification.getAdmin().getId().equals(currentAdmin.getId()))
                .map(notification -> populateDocumentUrls(adminNotificationMapper.toDto(notification), notification))
                .sorted((a, b) -> {
                    // Sort by createdAt descending (newest first)
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    return 0;
                })
                .toList();
        
        log.info("Found {} notifications for admin: {} (ID: {})", notifications.size(), email, currentAdmin.getId());
        
        return ResponsePayload.builder()
                .message("Notifications retrieved successfully")
                .status(200)
                .data(Map.of("notifications", notifications))
                .build();
    }
    
    /**
     * Populate document URLs in notification DTO based on notification type
     */
    private AdminNotificationDto populateDocumentUrls(AdminNotificationDto dto, AdminNotification notification) {
        List<String> documentUrls = new ArrayList<>();
        
        try {
            if (notification.getType() == com.lebvest.model.enums.AdminNotificationType.SIGNUP_REQUEST) {
                // Extract from CompanySignupRequest
                // If request was approved, files may have been moved to accepted, so check Company documents first
                if (notification.getCompany() != null && notification.getCompany().getDocuments() != null && !notification.getCompany().getDocuments().isEmpty()) {
                    // Use Company documents (accepted paths) if available - these are the correct paths after approval
                    documentUrls = notification.getCompany().getDocuments().stream()
                            .map(path -> convertPathToUrl(path))
                            .filter(url -> url != null)
                            .collect(Collectors.toList());
                } else if (notification.getRequest() != null && notification.getRequest().getDocuments() != null) {
                    // Fallback to request documents (pending paths) for unapproved requests
                    documentUrls = notification.getRequest().getDocuments().stream()
                            .map(path -> convertPathToUrl(path))
                            .filter(url -> url != null)
                            .collect(Collectors.toList());
                }
            } else if (notification.getType() == com.lebvest.model.enums.AdminNotificationType.PROJECT_PROPOSAL) {
                // Extract from Investment documents
                if (notification.getInvestment() != null && notification.getInvestment().getDocuments() != null) {
                    documentUrls = notification.getInvestment().getDocuments().stream()
                            .map(doc -> doc.getUrl())
                            .map(path -> convertPathToUrl(path))
                            .filter(url -> url != null)
                            .collect(Collectors.toList());
                }
            } else if (notification.getType() == com.lebvest.model.enums.AdminNotificationType.VERIFICATION_REQUEST) {
                // Extract from CompanyVerificationDocuments
                if (notification.getCompany() != null) {
                    CompanyVerificationDocuments docs = verificationDocumentsRepository.findByCompany(notification.getCompany()).orElse(null);
                    if (docs != null) {
                        documentUrls = adminNotificationMapper.extractVerificationDocumentUrls(docs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting document URLs for notification {}: {}", notification.getId(), e.getMessage());
        }
        
        dto.setDocumentUrls(documentUrls);
        return dto;
    }
    
    /**
     * Convert file path to accessible URL
     */
    private String convertPathToUrl(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        // If already a full URL, return as is
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }
        
        // Convert relative path to URL
        // Format: http://localhost:8080/api/files/{path}
        return varsConfig.getFrontendUrl().replace(":3000", ":8080") + "/api/files/" + filePath.replace("\\", "/");
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
        var notificationDto = populateDocumentUrls(adminNotificationMapper.toDto(notification), notification);

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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminAnalyticsDto getEnhancedAnalytics() {
        // Basic stats
        long totalInvestors = investorRepository.count();
        long totalCompanies = companyRepo.count();
        long totalInvestments = investmentRepository.count();
        
        // Today's investments
        java.time.LocalDate today = java.time.LocalDate.now();
        java.math.BigDecimal totalInvestedToday = investorInvestmentRepository.findAll().stream()
                .filter(inv -> inv.getInvestedAt() != null && 
                        inv.getInvestedAt().equals(today))
                .map(inv -> inv.getAmount() != null ? inv.getAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        // This month's investments
        java.time.LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        java.math.BigDecimal totalInvestedThisMonth = investorInvestmentRepository.findAll().stream()
                .filter(inv -> inv.getInvestedAt() != null && 
                        inv.getInvestedAt().isAfter(firstDayOfMonth.minusDays(1)))
                .map(inv -> inv.getAmount() != null ? inv.getAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        // Top projects (by raised amount)
        List<AdminAnalyticsDto.TopProjectDto> topProjects = investmentRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.APPROVED)
                .sorted((a, b) -> b.getRaisedAmount().compareTo(a.getRaisedAmount()))
                .limit(10)
                .map(inv -> AdminAnalyticsDto.TopProjectDto.builder()
                        .id(inv.getId())
                        .title(inv.getTitle())
                        .companyName(inv.getCompany().getName())
                        .raisedAmount(inv.getRaisedAmount())
                        .targetAmount(inv.getTargetAmount())
                        .investorCount((long) investorInvestmentRepository.findByInvestmentId(inv.getId()).size())
                        .build())
                .collect(Collectors.toList());
        
        // Daily investments (last 30 days)
        Map<java.time.LocalDate, java.math.BigDecimal> dailyInvestments = new java.util.HashMap<>();
        for (int i = 29; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            java.math.BigDecimal dailyTotal = investorInvestmentRepository.findAll().stream()
                    .filter(inv -> inv.getInvestedAt() != null && 
                            inv.getInvestedAt().equals(date))
                    .map(inv -> inv.getAmount() != null ? inv.getAmount() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            dailyInvestments.put(date, dailyTotal);
        }
        
        // Investments by category
        Map<String, Long> investmentsByCategory = investmentRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.APPROVED)
                .collect(Collectors.groupingBy(
                        inv -> inv.getCategory() != null ? inv.getCategory().toString() : "Unknown",
                        Collectors.counting()
                ));
        
        // Investments by risk level
        Map<String, Long> investmentsByRiskLevel = investmentRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.APPROVED)
                .collect(Collectors.groupingBy(
                        inv -> inv.getRiskLevel() != null ? inv.getRiskLevel().toString() : "Unknown",
                        Collectors.counting()
                ));
        
        // Queue counts
        long pendingCompanyApprovals = companySignupRequestRepository.findAll().stream()
                .filter(req -> req.getRequestStatus() == SignupRequestStatus.PENDING)
                .count();
        
        long pendingInvestorApprovals = userRepo.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains(Role.INVESTOR) && 
                        !u.isEnabled())
                .count();
        
        // Pending payouts and returns (placeholder - implement based on your payout/return logic)
        long pendingPayouts = 0L; // TODO: Implement based on payout request status
        long pendingReturns = 0L; // TODO: Implement based on return request status
        
        return AdminAnalyticsDto.builder()
                .totalInvestors(totalInvestors)
                .totalCompanies(totalCompanies)
                .totalInvestments(totalInvestments)
                .totalInvestedToday(totalInvestedToday)
                .totalInvestedThisMonth(totalInvestedThisMonth)
                .topProjects(topProjects)
                .dailyInvestments(dailyInvestments)
                .investmentsByCategory(investmentsByCategory)
                .investmentsByRiskLevel(investmentsByRiskLevel)
                .pendingCompanyApprovals(pendingCompanyApprovals)
                .pendingInvestorApprovals(pendingInvestorApprovals)
                .pendingPayouts(pendingPayouts)
                .pendingReturns(pendingReturns)
                .build();
    }

    @Transactional
    public ResponsePayload approveVerificationDocuments(Long companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        log.info("Attempting to approve verification for company: {} (ID: {}), current status: {}", 
                company.getName(), companyId, company.getStatus());

        // Check if already fully verified
        if (company.getStatus() == CompanyStatus.FULLY_VERIFIED) {
            log.info("Company {} (ID: {}) is already FULLY_VERIFIED", company.getName(), companyId);
            return ResponsePayload.builder()
                    .status(200)
                    .message("Company is already fully verified.")
                    .data(Map.of("companyId", companyId, "status", "FULLY_VERIFIED"))
                    .build();
        }

        // Allow approval if company is in PENDING_DOCS or APPROVED status
        // (APPROVED allows re-approval if documents were resubmitted)
        if (company.getStatus() != CompanyStatus.PENDING_DOCS && company.getStatus() != CompanyStatus.APPROVED) {
            log.warn("Cannot approve verification for company {} (ID: {}). Current status: {}, expected: PENDING_DOCS or APPROVED", 
                    company.getName(), companyId, company.getStatus());
            throw new IllegalStateException("Company must be in PENDING_DOCS or APPROVED status to approve verification. Current status: " + company.getStatus());
        }

        CompanyVerificationDocuments docs = verificationDocumentsRepository.findByCompany(company)
                .orElseThrow(() -> new IllegalArgumentException("Verification documents not found"));

        log.info("Found verification documents for company: {} (ID: {}), isApproved: {}", 
                company.getName(), companyId, docs.getIsApproved());

        // Approve documents
        docs.setIsApproved(true);
        verificationDocumentsRepository.save(docs);
        
        // Create document history entry (if repository is available)
        // Note: This requires VerificationDocumentHistoryRepository to be injected
        // For now, we'll skip this to avoid breaking existing code

        // Update company status to FULLY_VERIFIED
        company.setStatus(CompanyStatus.FULLY_VERIFIED);
        Company savedCompany = companyRepo.save(company);
        
        // Verify the status was saved
        if (savedCompany.getStatus() != CompanyStatus.FULLY_VERIFIED) {
            log.error("CRITICAL: Company status was not saved correctly! Expected FULLY_VERIFIED but got: {}", 
                    savedCompany.getStatus());
            throw new IllegalStateException("Failed to update company status to FULLY_VERIFIED");
        }

        log.info("Company status updated to FULLY_VERIFIED for company: {} (ID: {}). Verified status: {}", 
                company.getName(), companyId, savedCompany.getStatus());

        // Update all related notifications to mark them as accepted
        List<AdminNotification> relatedNotifications = adminNotificationRepository.findAll().stream()
                .filter(notif -> notif.getCompany() != null && notif.getCompany().getId().equals(companyId))
                .filter(notif -> notif.getType() == com.lebvest.model.enums.AdminNotificationType.VERIFICATION_REQUEST)
                .filter(notif -> notif.getIsAccepted() == null) // Only update pending ones
                .toList();
        
        log.info("Found {} notification(s) to update for company {} (ID: {})", 
                relatedNotifications.size(), company.getName(), companyId);
        
        for (AdminNotification notification : relatedNotifications) {
            notification.setIsAccepted(true);
            adminNotificationRepository.save(notification);
            log.info("Updated notification {} to accepted for company {} (ID: {})", 
                    notification.getId(), company.getName(), companyId);
        }

        // Send email to company
        sendVerificationApprovalEmail(company);

        log.info("Verification documents approved for company: {} (ID: {})", company.getName(), companyId);

        return ResponsePayload.builder()
                .status(200)
                .message("Company verification approved. Company can now post projects.")
                .data(Map.of("companyId", companyId, "status", "FULLY_VERIFIED"))
                .build();
    }

    @Transactional
    public ResponsePayload rejectVerificationDocuments(Long companyId, String reason) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        log.info("Attempting to reject verification for company: {} (ID: {}), current status: {}", 
                company.getName(), companyId, company.getStatus());

        // Allow rejection if company is in PENDING_DOCS or APPROVED status
        if (company.getStatus() != CompanyStatus.PENDING_DOCS && company.getStatus() != CompanyStatus.APPROVED) {
            log.warn("Cannot reject verification for company {} (ID: {}). Current status: {}, expected: PENDING_DOCS or APPROVED", 
                    company.getName(), companyId, company.getStatus());
            throw new IllegalStateException("Company must be in PENDING_DOCS or APPROVED status to reject verification. Current status: " + company.getStatus());
        }

        CompanyVerificationDocuments docs = verificationDocumentsRepository.findByCompany(company)
                .orElseThrow(() -> new IllegalArgumentException("Verification documents not found"));

        log.info("Found verification documents for company: {} (ID: {}), isApproved: {}", 
                company.getName(), companyId, docs.getIsApproved());

        // Reject documents
        docs.setIsApproved(false);
        verificationDocumentsRepository.save(docs);

        // Update company status back to APPROVED (so they can resubmit)
        company.setStatus(CompanyStatus.APPROVED);
        Company savedCompany = companyRepo.save(company);
        
        // Verify the status was saved
        if (savedCompany.getStatus() != CompanyStatus.APPROVED) {
            log.error("CRITICAL: Company status was not saved correctly! Expected APPROVED but got: {}", 
                    savedCompany.getStatus());
            throw new IllegalStateException("Failed to update company status to APPROVED");
        }

        log.info("Company status updated to APPROVED for company: {} (ID: {}). Verified status: {}", 
                company.getName(), companyId, savedCompany.getStatus());

        // Update all related notifications to mark them as rejected
        List<AdminNotification> relatedNotifications = adminNotificationRepository.findAll().stream()
                .filter(notif -> notif.getCompany() != null && notif.getCompany().getId().equals(companyId))
                .filter(notif -> notif.getType() == com.lebvest.model.enums.AdminNotificationType.VERIFICATION_REQUEST)
                .filter(notif -> notif.getIsAccepted() == null) // Only update pending ones
                .toList();
        
        log.info("Found {} notification(s) to update for company {} (ID: {})", 
                relatedNotifications.size(), company.getName(), companyId);
        
        for (AdminNotification notification : relatedNotifications) {
            notification.setIsAccepted(false);
            adminNotificationRepository.save(notification);
            log.info("Updated notification {} to rejected for company {} (ID: {})", 
                    notification.getId(), company.getName(), companyId);
        }

        // Send rejection email to company
        sendVerificationRejectionEmail(company, reason);

        log.info("Verification documents rejected for company: {} (ID: {})", company.getName(), companyId);

        return ResponsePayload.builder()
                .status(200)
                .message("Company verification rejected. Company can resubmit documents.")
                .data(Map.of("companyId", companyId, "status", "APPROVED"))
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

    private void sendVerificationRejectionEmail(Company company, String reason) {
        try {
            String companyEmail = company.getUser().getEmail();
            String loginUrl = varsConfig.getFrontendUrl() + "/signin";

            Map<String, String> templateData = new HashMap<>();
            templateData.put("name", company.getUser().getName());
            templateData.put("companyName", company.getName());
            templateData.put("reason", reason != null ? reason : "Documents did not meet verification requirements.");
            templateData.put("loginUrl", loginUrl);

            String htmlContent = mailService.loadAndFormatEmailTemplate(templateData, "CompanyVerificationRejection");
            mailService.sendHtmlMail(companyEmail, "Verification Documents Rejected - Action Required", htmlContent);
            log.info("Verification rejection email sent to: {}", companyEmail);
        } catch (Exception e) {
            log.error("Failed to send verification rejection email: {}", e.getMessage(), e);
        }
    }

    private void sendProjectApprovalEmail(Company company, com.lebvest.model.entities.investment.Investment investment, String reviewNotes) {
        try {
            String companyEmail = company.getUser().getEmail();
            String dashboardUrl = varsConfig.getFrontendUrl() + "/company-dashboard";

            Map<String, String> templateData = new HashMap<>();
            templateData.put("name", company.getUser().getName());
            templateData.put("companyName", company.getName());
            templateData.put("projectTitle", investment.getTitle());
            templateData.put("dashboardUrl", dashboardUrl);
            if (reviewNotes != null && !reviewNotes.trim().isEmpty()) {
                templateData.put("reviewNotes", reviewNotes);
            } else {
                templateData.put("reviewNotes", "");
            }

            String htmlContent = mailService.loadAndFormatEmailTemplate(templateData, "ProjectApproval");
            mailService.sendHtmlMail(companyEmail, "Project Approved - " + investment.getTitle(), htmlContent);
            log.info("Project approval email sent to: {}", companyEmail);
        } catch (Exception e) {
            log.error("Failed to send project approval email: {}", e.getMessage(), e);
        }
    }

    private void sendProjectRejectionEmail(Company company, com.lebvest.model.entities.investment.Investment investment, String reason, String reviewNotes) {
        try {
            String companyEmail = company.getUser().getEmail();
            String dashboardUrl = varsConfig.getFrontendUrl() + "/company-dashboard";

            Map<String, String> templateData = new HashMap<>();
            templateData.put("name", company.getUser().getName());
            templateData.put("companyName", company.getName());
            templateData.put("projectTitle", investment.getTitle());
            templateData.put("reason", reason != null ? reason : "Project did not meet our requirements.");
            templateData.put("dashboardUrl", dashboardUrl);
            if (reviewNotes != null && !reviewNotes.trim().isEmpty()) {
                templateData.put("reviewNotes", reviewNotes);
            } else {
                templateData.put("reviewNotes", "");
            }

            String htmlContent = mailService.loadAndFormatEmailTemplate(templateData, "ProjectRejection");
            mailService.sendHtmlMail(companyEmail, "Project Rejected - " + investment.getTitle(), htmlContent);
            log.info("Project rejection email sent to: {}", companyEmail);
        } catch (Exception e) {
            log.error("Failed to send project rejection email: {}", e.getMessage(), e);
        }
    }

    // ========== PROJECT REVIEW METHODS ==========

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AdminProjectReviewDto> getPendingProjects(
            InvestmentStatus status,
            InvestmentCategory category,
            String search,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // First, get paginated investments without eager loading (for proper pagination)
        Page<com.lebvest.model.entities.investment.Investment> investmentsPage = 
                investmentRepository.findPendingInvestmentsForAdmin(status, category, search, pageable);
        
        // Then, load company and batch load all collections separately to avoid MultipleBagFetchException
        List<com.lebvest.model.entities.investment.Investment> investments = investmentsPage.getContent();
        if (!investments.isEmpty()) {
            List<Long> investmentIds = investments.stream()
                    .map(com.lebvest.model.entities.investment.Investment::getId)
                    .collect(Collectors.toList());
            
            // Load investments with company only (to avoid MultipleBagFetchException)
            List<com.lebvest.model.entities.investment.Investment> investmentsWithCompany = 
                    investmentRepository.findByIdsWithCompany(investmentIds);
            java.util.Map<Long, com.lebvest.model.entities.investment.Investment> investmentsMap = 
                    investmentsWithCompany.stream()
                            .collect(Collectors.toMap(
                                    com.lebvest.model.entities.investment.Investment::getId,
                                    inv -> inv,
                                    (existing, replacement) -> existing
                            ));
            
            // Batch load all collections separately
            List<com.lebvest.model.entities.investment.InvestmentHighlight> highlights = 
                    investmentHighlightRepository.findByInvestmentIds(investmentIds);
            List<com.lebvest.model.entities.investment.InvestmentFinancial> financials = 
                    investmentFinancialRepository.findByInvestmentIds(investmentIds);
            List<com.lebvest.model.entities.investment.InvestmentDocument> documents = 
                    investmentDocumentRepository.findByInvestmentIds(investmentIds);
            List<com.lebvest.model.entities.investment.InvestmentTeamMember> teamMembers = 
                    investmentTeamMemberRepository.findByInvestmentIds(investmentIds);
            List<com.lebvest.model.entities.investment.InvestmentUpdate> updates = 
                    investmentUpdateRepository.findByInvestmentIds(investmentIds);
            
            // Group collections by investment ID
            java.util.Map<Long, List<com.lebvest.model.entities.investment.InvestmentHighlight>> highlightsMap = 
                    highlights.stream().collect(Collectors.groupingBy(h -> h.getInvestment().getId()));
            java.util.Map<Long, List<com.lebvest.model.entities.investment.InvestmentFinancial>> financialsMap = 
                    financials.stream().collect(Collectors.groupingBy(f -> f.getInvestment().getId()));
            java.util.Map<Long, List<com.lebvest.model.entities.investment.InvestmentDocument>> documentsMap = 
                    documents.stream().collect(Collectors.groupingBy(d -> d.getInvestment().getId()));
            java.util.Map<Long, List<com.lebvest.model.entities.investment.InvestmentTeamMember>> teamMembersMap = 
                    teamMembers.stream().collect(Collectors.groupingBy(tm -> tm.getInvestment().getId()));
            java.util.Map<Long, List<com.lebvest.model.entities.investment.InvestmentUpdate>> updatesMap = 
                    updates.stream().collect(Collectors.groupingBy(u -> u.getInvestment().getId()));
            
            // Attach collections to investments
            investmentsWithCompany.forEach(inv -> {
                inv.getHighlights().clear();
                inv.getHighlights().addAll(highlightsMap.getOrDefault(inv.getId(), Collections.emptyList()));
                inv.getFinancials().clear();
                inv.getFinancials().addAll(financialsMap.getOrDefault(inv.getId(), Collections.emptyList()));
                inv.getDocuments().clear();
                inv.getDocuments().addAll(documentsMap.getOrDefault(inv.getId(), Collections.emptyList()));
                inv.getTeamMembers().clear();
                inv.getTeamMembers().addAll(teamMembersMap.getOrDefault(inv.getId(), Collections.emptyList()));
                inv.getUpdates().clear();
                inv.getUpdates().addAll(updatesMap.getOrDefault(inv.getId(), Collections.emptyList()));
            });
            
            // Replace with fully loaded investments
            investments = investments.stream()
                    .map(inv -> investmentsMap.getOrDefault(inv.getId(), inv))
                    .collect(Collectors.toList());
        }
        
        // Convert to DTOs
        List<AdminProjectReviewDto> dtoList = investments.stream()
                .map(this::convertToAdminReviewDto)
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(
                dtoList,
                pageable,
                investmentsPage.getTotalElements()
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminProjectReviewDto getProjectForReview(Long projectId) {
        com.lebvest.model.entities.investment.Investment investment = investmentRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Investment not found"));
        return convertToAdminReviewDto(investment);
    }

    @Transactional
    public AdminProjectReviewDto approveProject(Long projectId, ApproveProjectRequest request) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                com.lebvest.model.entities.investment.Investment investment = investmentRepository.findById(projectId)
                        .orElseThrow(() -> new IllegalArgumentException("Investment not found"));
                
                if (investment.getStatus() != InvestmentStatus.PENDING_REVIEW) {
                    throw new IllegalStateException("Investment is not in PENDING_REVIEW status. Current status: " + investment.getStatus());
                }
                
                // Initialize version if null (for existing records before migration)
                if (investment.getVersion() == null) {
                    investment.setVersion(0L);
                }
                
                investment.setStatus(InvestmentStatus.APPROVED);
                com.lebvest.model.entities.investment.Investment savedInvestment = investmentRepository.save(investment);
                
                Company company = savedInvestment.getCompany();
                String reviewNotes = request.getReviewNotes() != null && !request.getReviewNotes().trim().isEmpty() 
                        ? request.getReviewNotes() 
                        : null;
                
                // Build notification message
                String notificationMessage = "Your project \"" + savedInvestment.getTitle() + "\" has been approved and is now live on the platform.";
                if (reviewNotes != null) {
                    notificationMessage += "\n\nAdmin Notes: " + reviewNotes;
                }
                
                // Send SSE notification asynchronously (pass IDs to avoid detached entity issues)
                companyNotificationSseController.notifyCompany(
                        company.getId(),
                        CompanyNotificationType.PROJECT_APPROVED,
                        "Project Approved",
                        notificationMessage,
                        savedInvestment.getId()
                );
                
                // Send email notification
                sendProjectApprovalEmail(company, savedInvestment, reviewNotes);
                
                log.info("Project {} approved by admin. Notification sent to company {} (ID: {})", 
                        projectId, company.getName(), company.getId());
                
                return convertToAdminReviewDto(savedInvestment);
                
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("Failed to approve project {} after {} retries due to concurrent modification", projectId, maxRetries);
                    throw new IllegalStateException("Project status was modified by another process. Please refresh and try again.");
                }
                log.warn("Optimistic locking failure on project {} approval, retrying (attempt {}/{})", projectId, attempt, maxRetries);
                try {
                    Thread.sleep(100 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrupted during retry");
                }
            }
        }
        
        throw new IllegalStateException("Failed to approve project after retries");
    }

    @Transactional
    public AdminProjectReviewDto rejectProject(Long projectId, RejectProjectRequest request) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                com.lebvest.model.entities.investment.Investment investment = investmentRepository.findById(projectId)
                        .orElseThrow(() -> new IllegalArgumentException("Investment not found"));
                
                if (investment.getStatus() != InvestmentStatus.PENDING_REVIEW) {
                    throw new IllegalStateException("Investment is not in PENDING_REVIEW status. Current status: " + investment.getStatus());
                }
                
                // Initialize version if null (for existing records before migration)
                if (investment.getVersion() == null) {
                    investment.setVersion(0L);
                }
                
                investment.setStatus(InvestmentStatus.REJECTED);
                com.lebvest.model.entities.investment.Investment savedInvestment = investmentRepository.save(investment);
                
                Company company = savedInvestment.getCompany();
                String reason = request.getReason() != null && !request.getReason().trim().isEmpty() 
                        ? request.getReason() 
                        : "No reason provided";
                String reviewNotes = request.getReviewNotes() != null && !request.getReviewNotes().trim().isEmpty() 
                        ? request.getReviewNotes() 
                        : null;
                
                // Build notification message
                String notificationMessage = "Your project \"" + savedInvestment.getTitle() + "\" has been rejected.\n\nReason: " + reason;
                if (reviewNotes != null) {
                    notificationMessage += "\n\nAdmin Notes: " + reviewNotes;
                }
                
                // Send SSE notification asynchronously (pass IDs to avoid detached entity issues)
                companyNotificationSseController.notifyCompany(
                        company.getId(),
                        CompanyNotificationType.PROJECT_REJECTED,
                        "Project Rejected",
                        notificationMessage,
                        savedInvestment.getId()
                );
                
                // Send email notification
                sendProjectRejectionEmail(company, savedInvestment, reason, reviewNotes);
                
                log.info("Project {} rejected by admin. Reason: {}. Notification sent to company {} (ID: {})", 
                        projectId, reason, company.getName(), company.getId());
                
                return convertToAdminReviewDto(savedInvestment);
                
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("Failed to reject project {} after {} retries due to concurrent modification", projectId, maxRetries);
                    throw new IllegalStateException("Project status was modified by another process. Please refresh and try again.");
                }
                log.warn("Optimistic locking failure on project {} rejection, retrying (attempt {}/{})", projectId, attempt, maxRetries);
                try {
                    Thread.sleep(100 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrupted during retry");
                }
            }
        }
        
        throw new IllegalStateException("Failed to reject project after retries");
    }

    private AdminProjectReviewDto convertToAdminReviewDto(com.lebvest.model.entities.investment.Investment investment) {
        AdminProjectReviewDto.AdminProjectReviewDtoBuilder builder = AdminProjectReviewDto.builder()
                .id(investment.getId())
                .title(investment.getTitle())
                .companyName(investment.getCompany().getName())
                .companyId(investment.getCompany().getId())
                .description(investment.getDescription())
                .category(investment.getCategory())
                .riskLevel(investment.getRiskLevel())
                .expectedReturn(investment.getExpectedReturn())
                .minInvestment(investment.getMinInvestment())
                .targetAmount(investment.getTargetAmount())
                .raisedAmount(investment.getRaisedAmount())
                .location(investment.getLocation())
                .sector(investment.getCompany().getSector() != null ? investment.getCompany().getSector().getValue() : null)
                .investmentType(investment.getInvestmentType())
                .durationMonths(investment.getDurationMonths())
                .imageUrl(investment.getImageUrl())
                .fundingStage(investment.getFundingStage())
                .deadline(investment.getDeadline())
                .createdAt(investment.getCreatedAt())
                .submittedDate(investment.getCreatedAt()) // Use createdAt as submitted date
                .status(investment.getStatus());

        // Convert highlights
        if (investment.getHighlights() != null) {
            builder.highlights(investment.getHighlights().stream()
                    .map(com.lebvest.model.entities.investment.InvestmentHighlight::getHighlight)
                    .collect(java.util.stream.Collectors.toList()));
        }

        // Convert team members
        if (investment.getTeamMembers() != null) {
            builder.team(investment.getTeamMembers().stream()
                    .map(tm -> AdminProjectReviewDto.TeamMemberDto.builder()
                            .name(tm.getName())
                            .role(tm.getRole())
                            .bio(tm.getBio())
                            .imageUrl(tm.getImageUrl())
                            .build())
                    .collect(java.util.stream.Collectors.toList()));
        }

        // Convert financials
        if (investment.getFinancials() != null) {
            builder.financials(investment.getFinancials().stream()
                    .map(f -> AdminProjectReviewDto.FinancialDto.builder()
                            .revenue(f.getRevenue())
                            .expenses(f.getExpenses())
                            .profit(f.getProfit())
                            .year(f.getYear())
                            .build())
                    .collect(java.util.stream.Collectors.toList()));
        }

        // Convert documents
        if (investment.getDocuments() != null) {
            builder.documents(investment.getDocuments().stream()
                    .map(d -> AdminProjectReviewDto.DocumentDto.builder()
                            .title(d.getTitle())
                            .type(d.getType())
                            .url(d.getUrl())
                            .build())
                    .collect(java.util.stream.Collectors.toList()));
        }

        // Convert updates
        if (investment.getUpdates() != null) {
            builder.updates(investment.getUpdates().stream()
                    .map(u -> AdminProjectReviewDto.UpdateDto.builder()
                            .date(u.getUpdateDate())
                            .title(u.getTitle())
                            .content(u.getContent())
                            .build())
                    .collect(java.util.stream.Collectors.toList()));
        }

        return builder.build();
    }

    // ========== USER MANAGEMENT METHODS ==========

    @org.springframework.transaction.annotation.Transactional(readOnly = true, timeout = 30)
    public Page<UserDto> getAllUsers(
            Role role,
            String status,
            String search,
            int page,
            int size) {
        try {
            log.info("Fetching users - page: {}, size: {}, role: {}, status: {}, search: {}", page, size, role, status, search);
            
            // Use pagination at database level to avoid loading all users
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
            Page<User> userPage;
            
            // Normalize search to use prefix search (better for indexes)
            // If search is provided, use prefix search instead of LIKE '%...%'
            String normalizedSearch = null;
            if (search != null && !search.trim().isEmpty()) {
                normalizedSearch = search.trim();
            }
            
            // Determine status filters
            Boolean enabledFilter = null;
            Boolean lockedFilter = null;
            if (status != null && !status.equals("All")) {
                if ("active".equals(status.toLowerCase())) {
                    enabledFilter = true;
                    lockedFilter = false;
                } else if ("inactive".equals(status.toLowerCase())) {
                    enabledFilter = false;
                } else if ("locked".equals(status.toLowerCase())) {
                    lockedFilter = true;
                }
            }
            
            // Use optimized database queries based on filters
            if (role == null && enabledFilter == null && lockedFilter == null && normalizedSearch == null) {
                // No filters - simple pagination
                userPage = userRepo.findAll(pageable);
            } else if (role != null && enabledFilter != null && normalizedSearch != null) {
                // All filters: role, search, enabled
                userPage = userRepo.findByRoleAndSearchAndEnabled(role, normalizedSearch, enabledFilter, pageable);
            } else if (role != null && lockedFilter != null && normalizedSearch != null) {
                // All filters: role, search, locked
                userPage = userRepo.findByRoleAndSearchAndLocked(role, normalizedSearch, lockedFilter, pageable);
            } else if (role != null && enabledFilter != null) {
                // Role and enabled
                userPage = userRepo.findByRoleAndEnabled(role, enabledFilter, pageable);
            } else if (role != null && lockedFilter != null) {
                // Role and locked
                userPage = userRepo.findByRoleAndLocked(role, lockedFilter, pageable);
            } else if (role != null && normalizedSearch != null) {
                // Role and search
                userPage = userRepo.findByRoleAndSearch(role, normalizedSearch, pageable);
            } else if (role != null) {
                // Only role
                userPage = userRepo.findByRole(role, pageable);
            } else if (normalizedSearch != null && enabledFilter != null) {
                // Search and enabled
                userPage = userRepo.findBySearchAndEnabled(normalizedSearch, enabledFilter, pageable);
            } else if (normalizedSearch != null && lockedFilter != null) {
                // Search and locked
                userPage = userRepo.findBySearchAndLocked(normalizedSearch, lockedFilter, pageable);
            } else if (normalizedSearch != null) {
                // Only search
                userPage = userRepo.findBySearch(normalizedSearch, pageable);
            } else if (enabledFilter != null) {
                // Only enabled
                userPage = userRepo.findByEnabled(enabledFilter, pageable);
            } else if (lockedFilter != null) {
                // Only locked
                userPage = userRepo.findByLocked(lockedFilter, pageable);
            } else {
                // Fallback to simple pagination
                userPage = userRepo.findAll(pageable);
            }
            
            // Get users from page - roles are already loaded via EntityGraph in separate query
            List<User> users = userPage.getContent();
            
            // Eagerly load roles for all users in a single query (if not already loaded)
            if (!users.isEmpty()) {
                List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
                List<User> usersWithRoles = userRepo.findAllByIdIn(userIds);
                // Create a map for quick lookup
                java.util.Map<Long, User> usersWithRolesMap = usersWithRoles.stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (existing, replacement) -> existing));
                // Replace users with fully loaded ones
                users = users.stream()
                        .map(u -> usersWithRolesMap.getOrDefault(u.getId(), u))
                        .collect(Collectors.toList());
            }
            
            // Batch load company and verification data
            java.util.Map<Long, Company> companyMap = new java.util.HashMap<>();
            java.util.Map<Long, CompanyVerificationDocuments> verificationDocsMap = new java.util.HashMap<>();
            
            if (!users.isEmpty()) {
                java.util.Map<Long, Company> batchCompanyMap = batchLoadCompanyData(users);
                if (batchCompanyMap != null) {
                    companyMap = batchCompanyMap;
                }
                // Also load verification documents map
                if (!companyMap.isEmpty()) {
                    List<Long> companyIds = companyMap.values().stream()
                            .map(Company::getId)
                            .collect(Collectors.toList());
                    List<CompanyVerificationDocuments> docs = verificationDocumentsRepository.findByCompanyIds(companyIds);
                    verificationDocsMap = docs.stream()
                            .collect(Collectors.toMap(
                                    doc -> doc.getCompany().getId(),
                                    doc -> doc,
                                    (existing, replacement) -> existing
                            ));
                }
            }
            
            // Create final maps for use in conversion
            final java.util.Map<Long, Company> finalCompanyMap = companyMap;
            final java.util.Map<Long, CompanyVerificationDocuments> finalVerificationDocsMap = verificationDocsMap;
            
            // Convert to DTOs using pre-loaded data
            List<UserDto> dtoList = users.stream()
                    .map(user -> convertToUserDto(user, finalCompanyMap, finalVerificationDocsMap))
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("Returning {} users (page {} of {})", dtoList.size(), page, userPage.getTotalPages());
            return new org.springframework.data.domain.PageImpl<>(
                    dtoList,
                    pageable,
                    userPage.getTotalElements()
            );
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Batch load company data for users to avoid N+1 queries
     * Returns a map of userId -> Company for quick lookup
     */
    private java.util.Map<Long, Company> batchLoadCompanyData(List<User> users) {
        // Get all company user IDs
        List<Long> companyUserIds = users.stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains(Role.COMPANY))
                .map(User::getId)
                .collect(Collectors.toList());
        
        if (companyUserIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        // Batch load companies for these users in a single query using IN clause
        List<Company> companies = companyRepo.findByUserIds(companyUserIds);
        
        if (companies.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        // Create a map: userId -> Company for quick lookup
        java.util.Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(
                        company -> company.getUser().getId(),
                        company -> company,
                        (existing, replacement) -> existing
                ));
        
        log.debug("Batch loaded {} companies for {} users", companies.size(), companyUserIds.size());
        return companyMap;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDto getUserDetails(Long userId) {
        // OPTIMIZED: Eagerly load user with roles to avoid lazy loading
        List<User> usersWithRoles = userRepo.findAllByIdIn(java.util.Collections.singletonList(userId));
        if (usersWithRoles.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = usersWithRoles.get(0);
        
        // OPTIMIZED: Pre-load company and verification documents to avoid N+1 queries
        java.util.Map<Long, Company> companyMap = new java.util.HashMap<>();
        java.util.Map<Long, CompanyVerificationDocuments> verificationDocsMap = new java.util.HashMap<>();
        
        // If user is a company, batch load company data
        if (user.getRoles() != null && user.getRoles().contains(Role.COMPANY)) {
            List<Long> userIds = java.util.Collections.singletonList(userId);
            List<Company> companies = companyRepo.findByUserIds(userIds);
            if (!companies.isEmpty()) {
                Company company = companies.get(0);
                companyMap.put(userId, company);
                
                // Load verification documents for this company
                if (company.getId() != null) {
                    List<Long> companyIds = java.util.Collections.singletonList(company.getId());
                    List<CompanyVerificationDocuments> docs = verificationDocumentsRepository.findByCompanyIds(companyIds);
                    if (!docs.isEmpty()) {
                        verificationDocsMap.put(company.getId(), docs.get(0));
                    }
                }
            }
        }
        
        return convertToUserDto(user, companyMap, verificationDocsMap);
    }

    @Transactional
    public UserDto updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        String status = request.getStatus().toLowerCase();
        if ("active".equals(status)) {
            user.setEnabled(true);
            user.setLocked(false);
        } else if ("inactive".equals(status)) {
            user.setEnabled(false);
        } else if ("locked".equals(status)) {
            user.setLocked(true);
        } else {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        
        userRepo.save(user);
        log.info("User {} status updated to {}", userId, status);
        
        return convertToUserDto(user);
    }

    private UserDto convertToUserDto(User user) {
        return convertToUserDto(user, null, null);
    }
    
    private UserDto convertToUserDto(User user, 
                                     java.util.Map<Long, Company> companyMap,
                                     java.util.Map<Long, CompanyVerificationDocuments> verificationDocsMap) {
        String status = determineUserStatus(user);
        UserDto.UserDtoBuilder builder = UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles())
                .status(status)
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .locked(user.isLocked());
        
        // If user is a company, include company verification information
        if (user.getRoles() != null && user.getRoles().contains(Role.COMPANY)) {
            Company company = null;
            if (companyMap != null) {
                // Use pre-loaded company from map
                company = companyMap.get(user.getId());
            } else {
                // Fallback to query if map not provided (for single user lookups)
                company = companyRepo.findByUser(user).orElse(null);
            }
            
            if (company != null) {
                builder.companyId(company.getId())
                       .companyStatus(company.getStatus());
                
                // Check verification documents approval status
                CompanyVerificationDocuments verificationDocs = null;
                if (verificationDocsMap != null && company.getId() != null) {
                    // Use pre-loaded verification docs from map
                    verificationDocs = verificationDocsMap.get(company.getId());
                } else if (company.getId() != null) {
                    // Fallback to query if map not provided
                    verificationDocs = verificationDocumentsRepository.findByCompany(company).orElse(null);
                }
                
                if (verificationDocs != null) {
                    builder.verificationDocumentsApproved(verificationDocs.getIsApproved());
                } else {
                    builder.verificationDocumentsApproved(false);
                }
            }
        }
        
        // Include online presence information
        // OPTIMIZED: Batch load all user activity statuses to avoid N queries
        // For now, keep individual lookups (they're fast - ConcurrentHashMap)
        // TODO: Consider caching user activity status in Redis or batch loading
        boolean isOnline = userActivityService.isUserOnline(user.getId());
        builder.isOnline(isOnline);
        if (isOnline) {
            builder.lastSeen(userActivityService.getLastActivity(user.getId()));
        }
        
        return builder.build();
    }

    private String determineUserStatus(User user) {
        if (user.isLocked()) {
            return "locked";
        }
        if (!user.isEnabled()) {
            return "inactive";
        }
        // Check if it's a company with pending signup
        // This is a simplified check - you might want to enhance this
        return "active";
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<com.lebvest.model.entities.company.CompanySignupRequest> getPendingCompanyApprovals(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return companySignupRequestRepository.findAll().stream()
                .filter(req -> req.getRequestStatus() == SignupRequestStatus.PENDING)
                .collect(Collectors.toList())
                .stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList())
                .stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(
                                list,
                                pageable,
                                companySignupRequestRepository.findAll().stream()
                                        .filter(req -> req.getRequestStatus() == SignupRequestStatus.PENDING)
                                        .count()
                        )
                ));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<UserDto> getPendingInvestorApprovals(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<User> pendingInvestors = userRepo.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains(Role.INVESTOR) && 
                        !u.isEnabled())
                .collect(Collectors.toList());
        
        List<UserDto> dtoList = pendingInvestors.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::convertToUserDto)
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(
                dtoList,
                pageable,
                pendingInvestors.size()
        );
    }
}
