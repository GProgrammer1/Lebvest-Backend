package com.lebvest.controller;

import com.lebvest.model.dto.CreateGoalRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.UpdateGoalRequest;
import com.lebvest.model.dto.investor.InvestorDashboardDto;
import com.lebvest.model.dto.investor.InvestorNotificationDto;
import com.lebvest.model.dto.investor.InvestorPreferenceDto;
import com.lebvest.model.dto.investor.InvestorProfileDto;
import com.lebvest.model.dto.investor.UpdateInvestorPreferenceRequest;
import com.lebvest.model.dto.investor.UpdateInvestorProfileRequest;
import com.lebvest.model.dto.investor.ChangePasswordRequest;
import com.lebvest.service.InvestorService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/investors/me")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173", "http://127.0.0.1:5173"})
public class InvestorController {

    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ResponsePayload> getDashboard() {
        var dashboard = investorService.getCurrentInvestorDashboard();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor dashboard fetched successfully")
                        .data(Map.of("dashboard", dashboard))
                        .build()
        );
    }

    @GetMapping("/investments")
    public ResponseEntity<ResponsePayload> getInvestments() {
        var investments = investorService.getCurrentInvestorInvestments();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor investments fetched successfully")
                        .data(Map.of("investments", investments))
                        .build()
        );
    }

    @GetMapping("/watchlist")
    public ResponseEntity<ResponsePayload> getWatchlist() {
        var watchlist = investorService.getCurrentInvestorWatchlist();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor watchlist fetched successfully")
                        .data(Map.of("watchlist", watchlist))
                        .build()
        );
    }

    @GetMapping("/goals")
    public ResponseEntity<ResponsePayload> getGoals() {
        var goals = investorService.getCurrentInvestorGoals();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor goals fetched successfully")
                        .data(Map.of("goals", goals))
                        .build()
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<ResponsePayload> getProfile() {
        InvestorProfileDto profile = investorService.getCurrentInvestorProfile();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor profile fetched successfully")
                        .data(Map.of("profile", profile))
                        .build()
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<ResponsePayload> updateProfile(@RequestBody @Valid UpdateInvestorProfileRequest request) {
        InvestorProfileDto profile = investorService.updateCurrentInvestorProfile(request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor profile updated successfully")
                        .data(Map.of("profile", profile))
                        .build()
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<ResponsePayload> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        investorService.changePassword(request);
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
        log.info("=== Profile Image Upload Endpoint Reached ===");
        log.info("Received profile image upload request. File name: {}, Size: {}, Content type: {}", 
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
            String imageUrl = investorService.uploadProfileImage(file);
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

    @GetMapping("/preferences")
    public ResponseEntity<ResponsePayload> getPreferences() {
        InvestorPreferenceDto preferences = investorService.getCurrentInvestorPreferences();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor preferences fetched successfully")
                        .data(Map.of("preferences", preferences))
                        .build()
        );
    }

    @PutMapping("/preferences")
    public ResponseEntity<ResponsePayload> updatePreferences(@RequestBody @Valid UpdateInvestorPreferenceRequest request) {
        InvestorPreferenceDto preferences = investorService.updateCurrentInvestorPreferences(request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor preferences updated successfully")
                        .data(Map.of("preferences", preferences))
                        .build()
        );
    }

    @GetMapping("/notifications")
    public ResponseEntity<ResponsePayload> getNotifications() {
        List<InvestorNotificationDto> notifications = investorService.getCurrentInvestorNotifications();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor notifications fetched successfully")
                        .data(Map.of("notifications", notifications))
                        .build()
        );
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<ResponsePayload> markNotificationAsRead(@PathVariable Long id) {
        InvestorNotificationDto notification = investorService.markNotificationAsRead(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Notification marked as read successfully")
                        .data(Map.of("notification", notification))
                        .build()
        );
    }

    @PostMapping("/goals")
    public ResponseEntity<ResponsePayload> createGoal(@RequestBody @Valid CreateGoalRequest request) {
        InvestorDashboardDto.InvestorGoalDto goal = investorService.createGoal(request);
        return ResponseEntity.status(201).body(
                ResponsePayload.builder()
                        .status(201)
                        .message("Goal created successfully")
                        .data(Map.of("goal", goal))
                        .build()
        );
    }

    @PutMapping("/goals/{id}")
    public ResponseEntity<ResponsePayload> updateGoal(
            @PathVariable Long id,
            @RequestBody @Valid UpdateGoalRequest request) {
        InvestorDashboardDto.InvestorGoalDto goal = investorService.updateGoal(id, request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Goal updated successfully")
                        .data(Map.of("goal", goal))
                        .build()
        );
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<ResponsePayload> deleteGoal(@PathVariable Long id) {
        investorService.deleteGoal(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Goal deleted successfully")
                        .data(Map.of())
                        .build()
        );
    }

    @GetMapping("/investments/{id}")
    public ResponseEntity<ResponsePayload> getInvestorInvestmentDetails(@PathVariable Long id) {
        InvestorDashboardDto.InvestorInvestmentDto investment = investorService.getInvestorInvestmentDetails(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor investment details retrieved successfully")
                        .data(Map.of("investment", investment))
                        .build()
        );
    }
}

