package com.lebvest.controller;

import com.lebvest.model.dto.*;
import com.lebvest.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/companies/me")
@CrossOrigin("http://localhost:3000")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/investments")
    public ResponseEntity<ResponsePayload> createInvestment(@RequestBody @Valid CreateInvestmentRequest request) {
        InvestmentDto investment = companyService.createInvestment(request);
        return ResponseEntity.status(201).body(
                ResponsePayload.builder()
                        .status(201)
                        .message("Investment created successfully")
                        .data(Map.of("investment", investment))
                        .build()
        );
    }

    @PutMapping("/investments/{id}")
    public ResponseEntity<ResponsePayload> updateInvestment(
            @PathVariable Long id,
            @RequestBody @Valid UpdateInvestmentRequest request) {
        InvestmentDto investment = companyService.updateInvestment(id, request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment updated successfully")
                        .data(Map.of("investment", investment))
                        .build()
        );
    }

    @GetMapping("/investments/{id}")
    public ResponseEntity<ResponsePayload> getInvestmentDetail(@PathVariable Long id) {
        InvestmentDetailDto detail = companyService.getInvestmentDetail(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment details fetched successfully")
                        .data(Map.of("investmentDetail", detail))
                        .build()
        );
    }

    @PostMapping("/investments/{id}/updates")
    public ResponseEntity<ResponsePayload> createInvestmentUpdate(
            @PathVariable Long id,
            @RequestBody @Valid CreateInvestmentUpdateRequest request) {
        InvestmentDto.UpdateDto update = companyService.createInvestmentUpdate(id, request);
        return ResponseEntity.status(201).body(
                ResponsePayload.builder()
                        .status(201)
                        .message("Investment update created successfully")
                        .data(Map.of("update", update))
                        .build()
        );
    }
}

