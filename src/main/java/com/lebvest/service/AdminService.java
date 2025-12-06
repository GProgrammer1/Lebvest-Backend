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
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.lebvest.model.entities.company.CompanyVerificationDocuments;
import com.lebvest.model.enums.CompanyStatus;
import com.lebvest.service.UserActivityService;
import com.lebvest.util.AdminNotificationMapper;
import jakarta.transaction.Transactional;
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
    private final InvestorRepository investorRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final LocalFileStorageService localFileStorageService;
    private final CompanyVerificationDocumentsRepository verificationDocumentsRepository;
    private final MailService mailService;
    private final UserActivityService userActivityService;
    private final AdminNotificationMapper adminNotificationMapper;
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
                        MailService mailService,
                        UserActivityService userActivityService,
                        AdminNotificationMapper adminNotificationMapper
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
        this.userActivityService = userActivityService;
        this.adminNotificationMapper = adminNotificationMapper;
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
                if (notification.getRequest() != null && notification.getRequest().getDocuments() != null) {
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

        // Update company status to FULLY_VERIFIED
        company.setStatus(CompanyStatus.FULLY_VERIFIED);
        companyRepo.save(company);

        log.info("Company status updated to FULLY_VERIFIED for company: {} (ID: {})", 
                company.getName(), companyId);

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
        companyRepo.save(company);

        log.info("Company status updated to APPROVED for company: {} (ID: {})", 
                company.getName(), companyId);

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

    // ========== PROJECT REVIEW METHODS ==========

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AdminProjectReviewDto> getPendingProjects(
            InvestmentStatus status,
            InvestmentCategory category,
            String search,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // If status is null, we want all projects (for "All" filter)
        // The repository query handles null status correctly
        Page<com.lebvest.model.entities.investment.Investment> investments = 
                investmentRepository.findPendingInvestmentsForAdmin(status, category, search, pageable);
        
        return investments.map(this::convertToAdminReviewDto);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminProjectReviewDto getProjectForReview(Long projectId) {
        com.lebvest.model.entities.investment.Investment investment = investmentRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Investment not found"));
        return convertToAdminReviewDto(investment);
    }

    @Transactional
    public AdminProjectReviewDto approveProject(Long projectId, ApproveProjectRequest request) {
        com.lebvest.model.entities.investment.Investment investment = investmentRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Investment not found"));
        
        if (investment.getStatus() != InvestmentStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Investment is not in PENDING_REVIEW status");
        }
        
        investment.setStatus(InvestmentStatus.APPROVED);
        investmentRepository.save(investment);
        
        // TODO: Send notification to company about approval
        log.info("Project {} approved by admin", projectId);
        
        return convertToAdminReviewDto(investment);
    }

    @Transactional
    public AdminProjectReviewDto rejectProject(Long projectId, RejectProjectRequest request) {
        com.lebvest.model.entities.investment.Investment investment = investmentRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Investment not found"));
        
        if (investment.getStatus() != InvestmentStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Investment is not in PENDING_REVIEW status");
        }
        
        investment.setStatus(InvestmentStatus.REJECTED);
        investmentRepository.save(investment);
        
        // TODO: Send notification to company about rejection with reason
        log.info("Project {} rejected by admin. Reason: {}", projectId, request.getReason());
        
        return convertToAdminReviewDto(investment);
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
            
            // If no filters, use simple pagination
            if (role == null && (status == null || status.equals("All")) && (search == null || search.isEmpty())) {
                userPage = userRepo.findAll(pageable);
            } else {
                // For now, still load all (but with timeout protection)
                // TODO: Optimize with proper JPA queries
                List<User> allUsersList = userRepo.findAll();
                log.info("Loaded {} users from database", allUsersList.size());
                
                // Apply filters
                java.util.stream.Stream<User> filteredStream = allUsersList.stream();
                
                // Filter by role
                if (role != null) {
                    filteredStream = filteredStream.filter(user -> user.getRoles().contains(role));
                }
                
                // Filter by status
                if (status != null && !status.equals("All")) {
                    filteredStream = filteredStream.filter(user -> {
                        String userStatus = determineUserStatus(user);
                        return userStatus.equals(status);
                    });
                }
                
                // Filter by search
                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    filteredStream = filteredStream.filter(user -> 
                        (user.getName() != null && user.getName().toLowerCase().contains(searchLower)) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower))
                    );
                }
                
                // Convert to list
                List<User> filteredList = filteredStream.collect(java.util.stream.Collectors.toList());
                
                // Manual pagination
                int start = page * size;
                int end = Math.min(start + size, filteredList.size());
                List<User> pageContent = start < filteredList.size() 
                        ? filteredList.subList(start, end) 
                        : new java.util.ArrayList<>();
                
                // Convert to DTOs
                List<UserDto> dtoList = pageContent.stream()
                        .map(this::convertToUserDto)
                        .collect(java.util.stream.Collectors.toList());
                
                return new org.springframework.data.domain.PageImpl<>(
                        dtoList,
                        pageable,
                        filteredList.size()
                );
            }
            
            // Convert to DTOs
            List<UserDto> dtoList = userPage.getContent().stream()
                    .map(this::convertToUserDto)
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDto getUserDetails(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return convertToUserDto(user);
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
            Company company = companyRepo.findByUser(user).orElse(null);
            if (company != null) {
                builder.companyId(company.getId())
                       .companyStatus(company.getStatus());
                
                // Check verification documents approval status
                CompanyVerificationDocuments verificationDocs = 
                        verificationDocumentsRepository.findByCompany(company).orElse(null);
                if (verificationDocs != null) {
                    builder.verificationDocumentsApproved(verificationDocs.getIsApproved());
                } else {
                    builder.verificationDocumentsApproved(false);
                }
            }
        }
        
        // Include online presence information
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
}
