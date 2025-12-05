package com.lebvest.controller;

import com.lebvest.model.dto.*;
import com.lebvest.model.dto.AdminProjectReviewDto;
import com.lebvest.model.dto.AdminStatisticsDto;
import com.lebvest.model.dto.AcceptSignupPayload;
import com.lebvest.model.dto.ApproveProjectRequest;
import com.lebvest.model.dto.RejectProjectRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.SignupRejectPayload;
import com.lebvest.model.dto.UpdateUserStatusRequest;
import com.lebvest.model.dto.UserDto;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentStatus;
import com.lebvest.model.enums.Role;
import com.lebvest.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
//@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    @PostMapping("/accept-request")
    public ResponseEntity<ResponsePayload> acceptRequest(@RequestBody AcceptSignupPayload payload) {

        ResponsePayload responsePayload = adminService.acceptSignupRequest(payload);
        return ResponseEntity.ok(
               responsePayload
        );
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
                        .build()
        );
    }

    @PostMapping("/approve-verification/{companyId}")
    public ResponseEntity<ResponsePayload> approveVerification(@PathVariable Long companyId) {
        ResponsePayload response = adminService.approveVerificationDocuments(companyId);
        return ResponseEntity.ok(response);
    }

    // ========== PROJECT REVIEW ENDPOINTS ==========

    @GetMapping("/projects/pending")
    public ResponseEntity<ResponsePayload> getPendingProjects(
            @RequestParam(required = false) InvestmentStatus status,
            @RequestParam(required = false) InvestmentCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        // Default to PENDING_REVIEW if no status specified
        if (status == null) {
            status = InvestmentStatus.PENDING_REVIEW;
        }
        
        Page<AdminProjectReviewDto> projects = adminService.getPendingProjects(status, category, search, page, size);
        
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
                        .build()
        );
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ResponsePayload> getProjectForReview(@PathVariable Long id) {
        AdminProjectReviewDto project = adminService.getProjectForReview(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Project details retrieved successfully")
                        .data(java.util.Map.of("project", project))
                        .build()
        );
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
                        .build()
        );
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
                        .build()
        );
    }

    // ========== USER MANAGEMENT ENDPOINTS ==========

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
                        .build()
        );
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ResponsePayload> getUserDetails(@PathVariable Long id) {
        UserDto user = adminService.getUserDetails(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("User details retrieved successfully")
                        .data(java.util.Map.of("user", user))
                        .build()
        );
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
                        .build()
        );
    }
}
