package com.lebvest.controller;

import com.lebvest.model.dto.*;
import com.lebvest.model.entities.investment.PayoutHistory;
import com.lebvest.model.entities.investment.PayoutRequest;
import com.lebvest.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PayoutController {
    private final PayoutService payoutService;

    // Investor endpoints
    @PostMapping("/investors/me/payouts/request/{investorInvestmentId}")
    public ResponseEntity<ResponsePayload> createPayoutRequest(@PathVariable Long investorInvestmentId) {
        PayoutRequest payoutRequest = payoutService.createPayoutRequest(investorInvestmentId);
        PayoutRequestDto dto = payoutService.convertToDto(payoutRequest);
        
        return ResponseEntity.status(201).body(
                ResponsePayload.builder()
                        .status(201)
                        .message("Payout request created successfully")
                        .data(Map.of("payoutRequest", dto))
                        .build()
        );
    }

    @GetMapping("/investors/me/payouts")
    public ResponseEntity<ResponsePayload> getInvestorPayouts(
            @RequestParam(required = false) String status) {
        List<PayoutRequest> requests = payoutService.getInvestorPayouts(status);
        List<PayoutRequestDto> dtos = requests.stream()
                .map(payoutService::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Payout requests retrieved successfully")
                        .data(Map.of("payoutRequests", dtos))
                        .build()
        );
    }

    @GetMapping("/investors/me/payouts/history")
    public ResponseEntity<ResponsePayload> getInvestorPayoutHistory() {
        List<PayoutHistory> history = payoutService.getInvestorPayoutHistory();
        List<PayoutHistoryDto> dtos = history.stream()
                .map(payoutService::convertHistoryToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Payout history retrieved successfully")
                        .data(Map.of("payoutHistory", dtos))
                        .build()
        );
    }

    // Company endpoints
    @GetMapping("/companies/me/payouts")
    public ResponseEntity<ResponsePayload> getCompanyPayouts(
            @RequestParam(required = false) String status) {
        List<PayoutRequest> requests = payoutService.getCompanyPayouts(status);
        List<PayoutRequestDto> dtos = requests.stream()
                .map(payoutService::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Payout requests retrieved successfully")
                        .data(Map.of("payoutRequests", dtos))
                        .build()
        );
    }

    @PostMapping("/companies/me/payouts/{payoutRequestId}/submit-evidence")
    public ResponseEntity<ResponsePayload> submitPayoutEvidence(
            @PathVariable Long payoutRequestId,
            @RequestParam("evidence") MultipartFile evidenceFile) {
        PayoutRequest payoutRequest = payoutService.submitPayoutEvidence(payoutRequestId, evidenceFile);
        PayoutRequestDto dto = payoutService.convertToDto(payoutRequest);
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Payout evidence submitted successfully")
                        .data(Map.of("payoutRequest", dto))
                        .build()
        );
    }

    // Admin endpoints
    @GetMapping("/admin/payouts")
    public ResponseEntity<ResponsePayload> getAdminPayouts(
            @RequestParam(required = false) String status) {
        List<PayoutRequest> requests = payoutService.getAdminPayouts(status);
        List<PayoutRequestDto> dtos = requests.stream()
                .map(payoutService::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Payout requests retrieved successfully")
                        .data(Map.of("payoutRequests", dtos))
                        .build()
        );
    }

    @PostMapping("/admin/payouts/{payoutRequestId}/approve")
    public ResponseEntity<ResponsePayload> approvePayout(
            @PathVariable Long payoutRequestId,
            @RequestBody(required = false) ApprovePayoutRequest request) {
        String adminNotes = request != null ? request.getAdminNotes() : null;
        PayoutRequest payoutRequest = payoutService.approvePayout(payoutRequestId, adminNotes);
        PayoutRequestDto dto = payoutService.convertToDto(payoutRequest);
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Payout approved successfully")
                        .data(Map.of("payoutRequest", dto))
                        .build()
        );
    }

    @PostMapping("/admin/payouts/{payoutRequestId}/reject")
    public ResponseEntity<ResponsePayload> rejectPayout(
            @PathVariable Long payoutRequestId,
            @RequestBody @Valid RejectPayoutRequest request) {
        PayoutRequest payoutRequest = payoutService.rejectPayout(payoutRequestId, request.getRejectionReason());
        PayoutRequestDto dto = payoutService.convertToDto(payoutRequest);
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Payout rejected successfully")
                        .data(Map.of("payoutRequest", dto))
                        .build()
        );
    }
}
