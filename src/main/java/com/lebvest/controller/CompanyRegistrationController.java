package com.lebvest.controller;

import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.service.CompanyRegistrationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/company")
public class CompanyRegistrationController {

    private final CompanyRegistrationService companyRegistrationService;

    public CompanyRegistrationController(CompanyRegistrationService companyRegistrationService) {
        this.companyRegistrationService = companyRegistrationService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponsePayload> registerCompany(@RequestBody @Valid CompanyRegistrationRequest companyRegistrationRequest) {
        log.info("CompanyRegistrationController - Received registration request");
        log.info("CompanyRegistrationController - Company name: {}", companyRegistrationRequest.getCompanyName());
        log.info("CompanyRegistrationController - Email: {}", companyRegistrationRequest.getEmail());
        log.info("CompanyRegistrationController - Name: {}", companyRegistrationRequest.getName());
        log.info("CompanyRegistrationController - Password present: {}, Password length: {}", 
                companyRegistrationRequest.getPassword() != null, 
                companyRegistrationRequest.getPassword() != null ? companyRegistrationRequest.getPassword().length() : 0);
        log.info("CompanyRegistrationController - Sector: {}", companyRegistrationRequest.getSector());
        log.info("CompanyRegistrationController - Location: {}", companyRegistrationRequest.getLocation());
        log.info("CompanyRegistrationController - Founded year: {}", companyRegistrationRequest.getFoundedYear());
        
        String token = companyRegistrationService.registerCompany(companyRegistrationRequest, null);
        return ResponseEntity.ok(
                ResponsePayload
                        .builder()
                        .message("Company registered successfully!")
                        .status(201)
                        .data(Map.of("token", token))
                        .build()
        );
    }
}
