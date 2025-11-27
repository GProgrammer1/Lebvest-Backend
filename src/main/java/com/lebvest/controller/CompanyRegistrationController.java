package com.lebvest.controller;

import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.service.CompanyRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping(value = "/register", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ResponsePayload> registerCompany(@ModelAttribute CompanyRegistrationRequest companyRegistrationRequest, BindingResult bindingResult) {

        log.info("Request to registerCompany is {}", companyRegistrationRequest.toString());
        String token = companyRegistrationService.registerCompany(companyRegistrationRequest, bindingResult);
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
