package com.lebvest.controller;

import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.investor.InvestorProfileDto;
import com.lebvest.service.InvestorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/investors")
@CrossOrigin("http://localhost:3000")
public class PublicInvestorController {

    private final InvestorService investorService;

    public PublicInvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsePayload> getPublicInvestorProfile(@PathVariable Long id) {
        InvestorProfileDto profile = investorService.getPublicInvestorProfile(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Public investor profile retrieved successfully")
                        .data(Map.of("profile", profile))
                        .build()
        );
    }
}

