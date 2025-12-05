package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.controller.AdminNotificationSseController;
import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.CompanySignupRequestRepository;
import com.lebvest.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.*;

@Service
public class CompanyRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(CompanyRegistrationService.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanySignupRequestRepository companySignupRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LocalFileStorageService localFileStorageService;
    private final MailService mailService;
    private final VarsConfig varsConfig;
    private final AdminNotificationSseController adminNotificationSseController;

    public CompanyRegistrationService(UserRepository userRepository,
                                      CompanyRepository companyRepository,
                                      CompanySignupRequestRepository companySignupRequestRepository,
                                      PasswordEncoder passwordEncoder,
                                      JwtService jwtService,
                                      LocalFileStorageService localFileStorageService,
                                      MailService mailService,
                                      VarsConfig varsConfig,
                                      AdminNotificationSseController adminNotificationSseController) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companySignupRequestRepository = companySignupRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.localFileStorageService = localFileStorageService;
        this.mailService = mailService;
        this.varsConfig = varsConfig;
        this.adminNotificationSseController = adminNotificationSseController;
    }

    @Transactional
    public void registerCompany(CompanyRegistrationRequest req, BindingResult bindingResult) {
        log.info("CompanyRegistrationService - Starting company registration for: {}", req.getCompanyName());
        log.info("CompanyRegistrationService - Email: {}, Name: {}", req.getEmail(), req.getName());
        log.info("CompanyRegistrationService - Password present: {}, Password length: {}", 
                req.getPassword() != null, req.getPassword() != null ? req.getPassword().length() : 0);
        
        // Validation is handled by @Valid annotation in the controller
        // This check is only for backward compatibility with @ModelAttribute requests
        if (bindingResult != null && bindingResult.hasErrors()) {
            StringBuilder errorMessages = new StringBuilder("Validation failed: <br>");
            bindingResult.getAllErrors().forEach(error ->
                    errorMessages.append(error.getDefaultMessage()).append("<br>")
            );
            throw new IllegalArgumentException(errorMessages.toString());
        }

        // Explicit password validation
        if (req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            log.error("CompanyRegistrationService - Password is null or empty!");
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // Check if user already exists
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("User with this email already exists");
        }

        // Check if company name already exists
        if (companyRepository.findByName(req.getCompanyName()).isPresent()) {
            throw new ConflictException("Company with this name already exists");
        }

        // Check if there's already a pending signup request for this email
        if (companySignupRequestRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("A signup request with this email already exists");
        }

        // Save files to local storage in pending folder
        List<String> documentPaths = new ArrayList<>();
        if (req.getDocuments() != null && req.getDocuments().length > 0) {
            UUID requestId = UUID.randomUUID();
            documentPaths = localFileStorageService.savePendingFiles(requestId, req.getDocuments());
        }

        // Create CompanySignupRequest (not Company yet - pending admin approval)
        CompanySignupRequest signupRequest = CompanySignupRequest.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword())) // Store encoded password
                .companyName(req.getCompanyName())
                .sector(req.getSector())
                .customSector(req.getCustomSector())
                .foundedYear(req.getFoundedYear())
                .governorate(req.getGovernorate())
                .city(req.getCity())
                .phoneNumber(req.getPhoneNumber())
                .website(req.getWebsite())
                .documents(documentPaths)
                .build();
        
        // Save and flush to ensure the request has an ID before creating notifications
        companySignupRequestRepository.saveAndFlush(signupRequest);
        log.info("Company signup request created with ID: {}", signupRequest.getId());

        // Send email to admin
        sendAdminNotificationEmail(signupRequest);

        // Create admin notification (request is now saved and has an ID)
        adminNotificationSseController.notifyAllAdmins(signupRequest);
    }

    private void sendAdminNotificationEmail(CompanySignupRequest request) {
        try {
            String adminEmail = varsConfig.getAdminEmail();
            String adminDashboardUrl = varsConfig.getFrontendUrl() + "/admin-dashboard";
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("companyName", request.getCompanyName());
            templateData.put("representativeName", request.getName());
            templateData.put("email", request.getEmail());
            templateData.put("phoneNumber", request.getPhoneNumber());
            templateData.put("website", request.getWebsite());
            // Use custom sector if sector is OTHER, otherwise use the sector display name
            String sectorDisplay = request.getSector() != null 
                ? (request.getSector() == com.lebvest.model.enums.CompanySector.OTHER && request.getCustomSector() != null && !request.getCustomSector().trim().isEmpty()
                    ? request.getCustomSector() 
                    : request.getSector().toString())
                : "N/A";
            templateData.put("sector", sectorDisplay);
            templateData.put("foundedYear", String.valueOf(request.getFoundedYear()));
            templateData.put("governorate", request.getGovernorate());
            templateData.put("city", request.getCity());
            templateData.put("adminDashboardUrl", adminDashboardUrl);

            String htmlContent = mailService.loadAndFormatEmailTemplate(templateData, "CompanySignupAdminNotification");
            mailService.sendHtmlMail(adminEmail, "New Company Signup Request: " + request.getCompanyName(), htmlContent);
            
            log.info("Admin notification email sent to: {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send admin notification email: {}", e.getMessage(), e);
            // Don't throw - registration should still succeed even if email fails
        }
    }
}
