package com.lebvest.controller;

import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.service.CompanyRegistrationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/company")
public class CompanyRegistrationController {

    private final CompanyRegistrationService companyRegistrationService;

    public CompanyRegistrationController(CompanyRegistrationService companyRegistrationService) {
        this.companyRegistrationService = companyRegistrationService;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponsePayload> registerCompany(
            @RequestPart("name") String name,
            @RequestPart("email") String email,
            @RequestPart("password") String password,
            @RequestPart("companyName") String companyName,
            @RequestPart("sector") String sector,
            @RequestPart(value = "customSector", required = false) String customSector,
            @RequestPart("foundedYear") String foundedYear,
            @RequestPart("governorate") String governorate,
            @RequestPart("city") String city,
            @RequestPart("phoneNumber") String phoneNumber,
            @RequestPart("website") String website,
            @RequestPart(value = "documents", required = false) MultipartFile[] documents) {
        
        log.info("CompanyRegistrationController - Received registration request");
        log.info("CompanyRegistrationController - Company name: {}", companyName);
        log.info("CompanyRegistrationController - Email: {}", email);
        log.info("CompanyRegistrationController - Name: {}", name);
        log.info("CompanyRegistrationController - Sector: {}", sector);
        log.info("CompanyRegistrationController - Custom Sector: {}", customSector);
        log.info("CompanyRegistrationController - Documents count: {}", documents != null ? documents.length : 0);
        
        // Convert sector string to enum (handle spaces and case)
        com.lebvest.model.enums.CompanySector sectorEnum;
        try {
            // Try to match by display name first (e.g., "Real Estate" -> REAL_ESTATE)
            sectorEnum = com.lebvest.model.enums.CompanySector.fromString(sector);
        } catch (IllegalArgumentException e) {
            // If that fails, try direct enum name conversion
            String sectorUpper = sector.toUpperCase().replace(" ", "_");
            sectorEnum = com.lebvest.model.enums.CompanySector.valueOf(sectorUpper);
        }
        
        // Build request object
        CompanyRegistrationRequest request = CompanyRegistrationRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .companyName(companyName)
                .sector(sectorEnum)
                .customSector(customSector)
                .foundedYear(Integer.parseInt(foundedYear))
                .governorate(governorate)
                .city(city)
                .phoneNumber(phoneNumber)
                .website(website)
                .documents(documents)
                .build();
        
        companyRegistrationService.registerCompany(request, null);
        
        return ResponseEntity.ok(
                ResponsePayload
                        .builder()
                        .message("Company signup request submitted successfully! Please wait for admin approval.")
                        .status(201)
                        .data(Map.of())
                        .build()
        );
    }
}
