package com.lebvest.controller;

import com.lebvest.model.dto.AdminStatisticsDto;
import com.lebvest.model.dto.AcceptSignupPayload;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.SignupRejectPayload;
import com.lebvest.service.AdminService;
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

    @PutMapping("/read-notification")
    public ResponseEntity<ResponsePayload> readNotification(@RequestBody Long id) {
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
}
