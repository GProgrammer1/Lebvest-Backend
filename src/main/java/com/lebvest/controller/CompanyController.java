package com.lebvest.controller;

import com.lebvest.model.dto.*;
import com.lebvest.model.dto.AcceptInvestmentRequestRequest;
import com.lebvest.model.dto.InvestmentRequestDto;
import com.lebvest.model.dto.RejectInvestmentRequestRequest;
import com.lebvest.model.dto.investor.ChangePasswordRequest;
import com.lebvest.model.entities.company.CompanyFinancial;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.service.CompanyService;
import com.lebvest.service.InvestmentRequestService;
import org.springframework.data.domain.Page;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/companies/me")
@CrossOrigin("http://localhost:3000")
public class CompanyController {

    private final CompanyService companyService;
    private final InvestmentRequestService investmentRequestService;

    public CompanyController(CompanyService companyService, InvestmentRequestService investmentRequestService) {
        this.companyService = companyService;
        this.investmentRequestService = investmentRequestService;
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

    @PostMapping("/financials")
    public ResponseEntity<ResponsePayload> addFinancial(@RequestBody @Valid AddCompanyFinancialRequest request) {
        CompanyFinancial financial = companyService.addFinancial(request);
        
        Map<String, Object> financialData = new HashMap<>();
        financialData.put("id", financial.getId());
        financialData.put("year", financial.getYear());
        financialData.put("revenue", financial.getRevenue());
        financialData.put("expenses", financial.getExpenses());
        financialData.put("profit", financial.getProfit());
        
        return ResponseEntity.status(201).body(
                ResponsePayload.builder()
                        .status(201)
                        .message("Financial data added successfully")
                        .data(Map.of("financial", financialData))
                        .build()
        );
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponsePayload> uploadDocument(@RequestPart("file") MultipartFile file) {
        log.info("Received file upload request. File name: {}, Size: {}, Content type: {}", 
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0,
                file != null ? file.getContentType() : "null");
        
        if (file == null || file.isEmpty()) {
            log.warn("File upload failed: file is null or empty");
            return ResponseEntity.badRequest().body(
                    ResponsePayload.builder()
                            .status(400)
                            .message("File is required. Please select a file to upload.")
                            .data(Map.of())
                            .build()
            );
        }
        
        try {
            String documentUrl = companyService.uploadDocument(file);
            return ResponseEntity.status(201).body(
                    ResponsePayload.builder()
                            .status(201)
                            .message("Document uploaded successfully")
                            .data(Map.of("documentUrl", documentUrl))
                            .build()
            );
        } catch (Exception e) {
            log.error("Error uploading document: {}", e.getMessage(), e);
            log.error("Exception type: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("Caused by: {}", e.getCause().getMessage());
            }
            String errorMessage = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred while uploading the document";
            return ResponseEntity.status(500).body(
                    ResponsePayload.builder()
                            .status(500)
                            .message(errorMessage)
                            .data(Map.of())
                            .build()
            );
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ResponsePayload> getCurrentCompanyProfile() {
        CompanyProfileDto profile = companyService.getCurrentCompanyProfile();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Company profile retrieved successfully")
                        .data(Map.of("profile", profile))
                        .build()
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<ResponsePayload> updateCompanyProfile(@RequestBody @Valid UpdateCompanyProfileRequest request) {
        CompanyProfileDto profile = companyService.updateCompanyProfile(request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Company profile updated successfully")
                        .data(Map.of("profile", profile))
                        .build()
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<ResponsePayload> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        companyService.changePassword(request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Password changed successfully")
                        .data(Map.of())
                        .build()
        );
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponsePayload> uploadProfileImage(@RequestPart("file") MultipartFile file) {
        log.info("Received company profile image upload request. File name: {}, Size: {}, Content type: {}",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0,
                file != null ? file.getContentType() : "null");

        if (file == null || file.isEmpty()) {
            log.warn("Profile image upload failed: file is null or empty");
            return ResponseEntity.badRequest().body(
                    ResponsePayload.builder()
                            .status(400)
                            .message("File is required. Please select an image file to upload.")
                            .data(Map.of())
                            .build()
            );
        }

        try {
            String imageUrl = companyService.uploadProfileImage(file);
            log.info("Profile image uploaded successfully: {}", imageUrl);
            return ResponseEntity.status(201).body(
                    ResponsePayload.builder()
                            .status(201)
                            .message("Profile image uploaded successfully")
                            .data(Map.of("imageUrl", imageUrl))
                            .build()
            );
        } catch (Exception e) {
            log.error("Error uploading profile image: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    ResponsePayload.builder()
                            .status(500)
                            .message("Failed to upload profile image: " + e.getMessage())
                            .data(Map.of())
                            .build()
            );
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ResponsePayload> getCompanyDashboard() {
        CompanyDashboardDto dashboard = companyService.getCompanyDashboard();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Company dashboard retrieved successfully")
                        .data(Map.of("dashboard", dashboard))
                        .build()
        );
    }

    @GetMapping("/investments")
    public ResponseEntity<ResponsePayload> getCompanyInvestments() {
        List<InvestmentDto> investments = companyService.getCompanyInvestments();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Company investments retrieved successfully")
                        .data(Map.of("investments", investments))
                        .build()
        );
    }

    @DeleteMapping("/investments/{id}")
    public ResponseEntity<ResponsePayload> deleteInvestment(@PathVariable Long id) {
        companyService.deleteInvestment(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment deleted successfully")
                        .data(Map.of())
                        .build()
        );
    }

    @GetMapping("/investors")
    public ResponseEntity<ResponsePayload> searchInvestors(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minPortfolio,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) InvestmentCategory category,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Page<InvestorSearchDto> investors = companyService.searchInvestors(
                query, minPortfolio, riskLevel, category, page, size
        );
        
        Map<String, Object> data = new HashMap<>();
        data.put("investors", investors.getContent());
        data.put("totalElements", investors.getTotalElements());
        data.put("totalPages", investors.getTotalPages());
        data.put("currentPage", investors.getNumber());
        data.put("pageSize", investors.getSize());
        data.put("hasNext", investors.hasNext());
        data.put("hasPrevious", investors.hasPrevious());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investors retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/verification")
    public ResponseEntity<ResponsePayload> submitVerificationDocuments(
            @RequestBody @Valid CompanyVerificationRequest request) {
        companyService.submitVerificationDocuments(request);
        return ResponseEntity.status(201).body(
                ResponsePayload.builder()
                        .status(201)
                        .message("Verification documents submitted successfully. Awaiting admin approval.")
                        .data(Map.of())
                        .build()
        );
    }

    @GetMapping("/verification")
    public ResponseEntity<ResponsePayload> getVerificationDocuments() {
        CompanyVerificationRequest docs = companyService.getVerificationDocuments();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Verification documents retrieved successfully")
                        .data(Map.of("documents", docs != null ? docs : Map.of()))
                        .build()
        );
    }

    // Investment Request Management Endpoints
    @GetMapping("/investment-requests")
    public ResponseEntity<ResponsePayload> getInvestmentRequests(
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        List<InvestmentRequestDto> requests = companyService.getInvestmentRequests(status);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment requests retrieved successfully")
                        .data(Map.of("requests", requests))
                        .build()
        );
    }

    @PostMapping("/investment-requests/{requestId}/accept")
    public ResponseEntity<ResponsePayload> acceptInvestmentRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) AcceptInvestmentRequestRequest request) {
        if (request == null) {
            request = new AcceptInvestmentRequestRequest();
        }
        var investmentRequest = investmentRequestService.acceptInvestmentRequest(requestId, request);
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("id", investmentRequest.getId());
        requestData.put("status", investmentRequest.getStatus().toString());
        requestData.put("acceptedAt", investmentRequest.getAcceptedAt());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment request accepted successfully")
                        .data(Map.of("investmentRequest", requestData))
                        .build()
        );
    }

    @PostMapping("/investment-requests/{requestId}/reject")
    public ResponseEntity<ResponsePayload> rejectInvestmentRequest(
            @PathVariable Long requestId,
            @RequestBody @Valid RejectInvestmentRequestRequest request) {
        var investmentRequest = investmentRequestService.rejectInvestmentRequest(requestId, request);
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("id", investmentRequest.getId());
        requestData.put("status", investmentRequest.getStatus().toString());
        requestData.put("rejectedAt", investmentRequest.getRejectedAt());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment request rejected successfully")
                        .data(Map.of("investmentRequest", requestData))
                        .build()
        );
    }
}

