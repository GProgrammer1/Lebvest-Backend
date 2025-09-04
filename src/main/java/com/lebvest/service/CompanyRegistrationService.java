package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.controller.AdminNotificationSseController;
import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.Attachment;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.dto.CompanySignupEmailEvent;
import com.lebvest.model.dto.CompanySignupUploadEvent;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.CompanySignupRequestRepository;
import com.lebvest.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompanyRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(CompanyRegistrationService.class);

    private final VarsConfig varsConfig;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanySignupRequestRepository companySignupRequestRepository;
    private final AdminNotificationSseController adminNotificationSseController;
    private final RabbitTemplate rabbitTemplate;

    public CompanyRegistrationService(VarsConfig varsConfig,
                                      UserRepository userRepository,
                                      CompanyRepository companyRepository,
                                      CompanySignupRequestRepository companySignupRequestRepository,
                                      AdminNotificationSseController adminNotificationSseController,
                                      RabbitTemplate rabbitTemplate) {
        this.varsConfig = varsConfig;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companySignupRequestRepository = companySignupRequestRepository;
        this.adminNotificationSseController = adminNotificationSseController;
        this.rabbitTemplate = rabbitTemplate;
    }

    public String registerCompany(CompanyRegistrationRequest req, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessages = new StringBuilder("Validation failed: <br>");
            bindingResult.getAllErrors().forEach(error ->
                    errorMessages.append(error.getDefaultMessage()).append("<br>")
            );
            throw new IllegalArgumentException(errorMessages.toString());
        }

        checkConflict(req);

        CompanySignupRequest signupRequest = buildSignupRequest(req);
        companySignupRequestRepository.save(signupRequest);

        // Offload uploading to queue: map MultipartFile[] -> Attachment[]
        var uploadQueueName = varsConfig.getSignupCompanyUploadQueueName();
        var files = Arrays.stream(req.getDocuments()).map(file -> {
            try {
                return new com.lebvest.model.events.Attachment(file.getOriginalFilename(), file.getBytes(), file.getContentType());
            } catch (IOException e) {
                throw new RuntimeException("Failed to buffer attachment: " + file.getOriginalFilename(), e);
            }
        }).toList();
        var uploadEvent = new com.lebvest.model.events.CompanySignupUploadEvent(signupRequest.getRequestId(), files);
        rabbitTemplate.convertAndSend(uploadQueueName, uploadEvent);

        // Persist the *intended* S3 keys right away so admins can see doc names
        String pendingPrefix = varsConfig.getPendingPrefix(signupRequest.getRequestId());
        List<String> uploadedKeys = Arrays.stream(req.getDocuments())
                .map(file -> pendingPrefix + "/" + file.getOriginalFilename())
                .collect(Collectors.toList());
        signupRequest.setDocuments(uploadedKeys);
        companySignupRequestRepository.save(signupRequest);

        // SSE notify admins
        adminNotificationSseController.notifyAllAdmins(signupRequest);

        // Offload email to admins to queue
        var emailQueueName = varsConfig.getSignupCompanyEmailQueueName();
        Map<String, String> templateData = Map.of(
                "name", safe(req.getName()),
                "email", safe(req.getEmail()),
                "companyName", safe(req.getCompanyName()),
                "description", safe(req.getDescription()),
                "sector", req.getSector() != null ? req.getSector().name() : "N/A"
        );
        var emailEvent = new com.lebvest.model.events.CompanySignupEmailEvent(
                "New Company Registration Request: " + req.getCompanyName(),
                "CompanyRegistrationEmail",
                templateData,
                files,   // forward attachments
                null     // null -> listener will send to admin email
        );
        rabbitTemplate.convertAndSend(emailQueueName, emailEvent);

        return "Request submitted successfully";
    }

    private String safe(String value) {
        return value != null ? value : "N/A";
    }

    private void checkConflict(CompanyRegistrationRequest req){
        var signupRequest = companySignupRequestRepository.findByEmail(req.getEmail());
        if (signupRequest.isPresent()) {
            throw new ConflictException("Company already exists");
        }
    }

    private CompanySignupRequest buildSignupRequest(CompanyRegistrationRequest req) {
        return CompanySignupRequest.builder()
                .requestId(UUID.randomUUID())
                .companyName(req.getCompanyName())
                .description(req.getDescription())
                .location(req.getLocation())
                .foundedYear(req.getFoundedYear())
                .name(req.getName())
                .password(req.getPassword())
                .sector(req.getSector())
                .email(req.getEmail())
                .build();
    }
}
