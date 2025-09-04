package com.lebvest.controller;

import com.lebvest.model.dto.InvestorRegistrationRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.service.InvestorRegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/investor")
public class InvestorRegistrationController {

    private final InvestorRegistrationService investorRegistrationService;

    public InvestorRegistrationController(InvestorRegistrationService investorRegistrationService) {
        this.investorRegistrationService = investorRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponsePayload> register(@RequestBody @Valid InvestorRegistrationRequest investorRegistrationRequest) {

        String token = investorRegistrationService.registerInvestor(investorRegistrationRequest);
        return ResponseEntity.ok(
                ResponsePayload
                        .builder()
                        .message("Investor registered successfully!")
                        .status(201)
                        .data(Map.of("token", token))
                        .build()
        );
    }
}
