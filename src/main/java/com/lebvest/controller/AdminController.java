package com.lebvest.controller;

import com.lebvest.model.dto.AdminProjectReviewDto;
import com.lebvest.model.dto.AdminStatisticsDto;
import com.lebvest.model.dto.AcceptSignupPayload;
import com.lebvest.model.dto.ApproveProjectRequest;
import com.lebvest.model.dto.RejectProjectRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.SignupRejectPayload;
import com.lebvest.model.dto.UpdateUserStatusRequest;
import com.lebvest.model.dto.UserDto;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentStatus;
import com.lebvest.model.enums.Role;
import com.lebvest.service.AdminService;
import com.lebvest.service.InvestorKycService;
import com.lebvest.model.enums.InvestorClassification;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
// @PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final InvestorKycService investorKycService;

    public AdminController(AdminService adminService, InvestorKycService investorKycService) {
        this.adminService = adminService;
        this.investorKycService = investorKycService;
    }

    @PostMapping("/accept-request")
    public ResponseEntity<ResponsePayload> acceptRequest(@RequestBody AcceptSignupPayload payload) {

        ResponsePayload responsePayload = adminService.acceptSignupRequest(payload);
        return ResponseEntity.ok(
                responsePayload);
    }

    @GetMapping("/notifications")
    public ResponseEntity<ResponsePayload> getNotifications() {
        ResponsePayload payload = adminService.getAllNotifications();
        return ResponseEntity.ok(payload);
    }

    @PutMapping("/reject-request")
    public ResponseEntity<ResponsePayload> rejectRequest(@RequestBody SignupRejectPayload rejectPayload) {
        ResponsePayload payload = adminService.rejectRequest(rejectPayload);
        return ResponseEntity.ok(payload);
    }

    @PutMapping("/read-notification/{id}")
    public ResponseEntity<ResponsePayload> readNotification(@PathVariable Long id) {
        ResponsePayload payload = adminService.readNotification(id);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/statistics")
    public ResponseEntity<ResponsePayload> getStatistics() {
        AdminStatisticsDto statistics = adminService.getStatistics();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Statistics retrieved successfully")
                        .data(Map.of("statistics", statistics))
                        .build());
    }

    @PostMapping("/approve-verification/{companyId}")
    public ResponseEntity<ResponsePayload> approveVerification(@PathVariable Long companyId) {
        ResponsePayload response = adminService.approveVerificationDocuments(companyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject-verification/{companyId}")
    public ResponseEntity<ResponsePayload> rejectVerification(
            @PathVariable Long companyId,
            @RequestBody(required = false) Map<String, String> requestBody) {
        String reason = requestBody != null ? requestBody.get("reason") : null;
        ResponsePayload response = adminService.rejectVerificationDocuments(companyId, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve-investor-verification/{investorId}")
    public ResponseEntity<ResponsePayload> approveInvestorVerification(@PathVariable Long investorId) {
        ResponsePayload response = adminService.approveInvestorVerification(investorId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject-investor-verification/{investorId}")
    public ResponseEntity<ResponsePayload> rejectInvestorVerification(
            @PathVariable Long investorId,
            @RequestBody(required = false) Map<String, String> requestBody) {
        String reason = requestBody != null ? requestBody.get("reason") : null;
        ResponsePayload response = adminService.rejectInvestorVerification(investorId, reason);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/projects/pending")
    public ResponseEntity<ResponsePayload> getPendingProjects(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) InvestmentCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        log.debug("Getting projects with status={}, category={}, search={}, page={}, size={}",
                status, category, search, page, size);

        InvestmentStatus investmentStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            if (status.equalsIgnoreCase("ALL")) {
                investmentStatus = null;
                log.info("Filtering projects: ALL statuses");
            } else {
                try {
                    investmentStatus = InvestmentStatus.valueOf(status.toUpperCase());
                    log.info("Filtering projects by status: {}", investmentStatus);
                } catch (IllegalArgumentException e) {
                    // Invalid status provided, default to PENDING_REVIEW
                    log.warn("Invalid status provided: '{}'. Defaulting to PENDING_REVIEW.", status);
                    investmentStatus = InvestmentStatus.PENDING_REVIEW;
                }
            }
        } else {
            // No status provided - default to PENDING_REVIEW for backward compatibility
            investmentStatus = InvestmentStatus.PENDING_REVIEW;
            log.info("No status provided, defaulting to PENDING_REVIEW");
        }

        Page<AdminProjectReviewDto> projects = adminService.getPendingProjects(investmentStatus, category, search, page,
                size);

        log.info("Retrieved {} projects with status {} (total: {}, page: {})",
                projects.getContent().size(), investmentStatus != null ? investmentStatus : "ALL",
                projects.getTotalElements(), projects.getNumber());

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("projects", projects.getContent());
        data.put("totalElements", projects.getTotalElements());
        data.put("totalPages", projects.getTotalPages());
        data.put("currentPage", projects.getNumber());
        data.put("pageSize", projects.getSize());
        data.put("hasNext", projects.hasNext());
        data.put("hasPrevious", projects.hasPrevious());

        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Pending projects retrieved successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ResponsePayload> getProjectForReview(@PathVariable Long id) {
        AdminProjectReviewDto project = adminService.getProjectForReview(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Project details retrieved successfully")
                        .data(java.util.Map.of("project", project))
                        .build());
    }

    @PostMapping("/projects/{id}/approve")
    public ResponseEntity<ResponsePayload> approveProject(
            @PathVariable Long id,
            @RequestBody @Valid ApproveProjectRequest request) {
        AdminProjectReviewDto project = adminService.approveProject(id, request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Project approved successfully")
                        .data(java.util.Map.of("project", project))
                        .build());
    }

    @PostMapping("/projects/{id}/reject")
    public ResponseEntity<ResponsePayload> rejectProject(
            @PathVariable Long id,
            @RequestBody @Valid RejectProjectRequest request) {
        AdminProjectReviewDto project = adminService.rejectProject(id, request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Project rejected successfully")
                        .data(java.util.Map.of("project", project))
                        .build());
    }


    @GetMapping("/users")
    public ResponseEntity<ResponsePayload> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        Page<UserDto> users = adminService.getAllUsers(role, status, search, page, size);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("users", users.getContent());
        data.put("totalElements", users.getTotalElements());
        data.put("totalPages", users.getTotalPages());
        data.put("currentPage", users.getNumber());
        data.put("pageSize", users.getSize());
        data.put("hasNext", users.hasNext());
        data.put("hasPrevious", users.hasPrevious());

        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Users retrieved successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ResponsePayload> getUserDetails(@PathVariable Long id) {
        UserDto user = adminService.getUserDetails(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("User details retrieved successfully")
                        .data(java.util.Map.of("user", user))
                        .build());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ResponsePayload> updateUserStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserStatusRequest request) {
        UserDto user = adminService.updateUserStatus(id, request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("User status updated successfully")
                        .data(java.util.Map.of("user", user))
                        .build());
    }

    @GetMapping("/investors/pending-verifications")
    public ResponseEntity<ResponsePayload> getPendingInvestorVerifications(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Page<Investor> investors = adminService.getPendingInvestorVerifications(page, size);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("investors", investors.getContent());
        data.put("totalElements", investors.getTotalElements());
        data.put("totalPages", investors.getTotalPages());
        data.put("currentPage", investors.getNumber());

        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Pending investor verifications retrieved successfully")
                        .data(data)
                        .build());
    }

    @PutMapping("/investors/{investorId}/kyc")
    public ResponseEntity<ResponsePayload> updateInvestorKyc(
            @PathVariable Long investorId,
            @RequestBody java.util.Map<String, Object> request) {
        InvestorClassification classification = InvestorClassification.valueOf(
                request.get("classification").toString().toUpperCase());
        String kycNotes = request.get("kycNotes") != null ? request.get("kycNotes").toString() : null;

        var investor = investorKycService.updateKycClassification(investorId, classification, kycNotes);

        java.util.Map<String, Object> investorData = new java.util.HashMap<>();
        investorData.put("id", investor.getId());
        investorData.put("classification", investor.getClassification());
        investorData.put("kycVerified", investor.getKycVerified());
        investorData.put("kycNotes", investor.getKycNotes());

        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor KYC updated successfully")
                        .data(java.util.Map.of("investor", investorData))
                        .build());
    }
}
