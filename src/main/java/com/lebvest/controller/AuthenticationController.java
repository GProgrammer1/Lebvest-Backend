package com.lebvest.controller;

import com.lebvest.model.dto.AuthenticationRequest;
import com.lebvest.model.dto.ForgotPasswordRequest;
import com.lebvest.model.dto.ResetPasswordRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.entities.investor.User;
import com.lebvest.service.AuthenticationService;
import com.lebvest.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationController(AuthenticationService authenticationService, RefreshTokenService refreshTokenService) {
        this.authenticationService = authenticationService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<ResponsePayload> login(@RequestBody @Valid AuthenticationRequest authenticationRequest) {
        Map<String, String> response = authenticationService.login(authenticationRequest);
        return ResponseEntity.ok(
                ResponsePayload
                        .builder()
                        .status(200)
                        .message("User authenticated successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResponsePayload> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        ResponsePayload response = authenticationService.forgotPassword(req);
        return ResponseEntity.ok(
                response
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponsePayload> resetPassword(@RequestBody ResetPasswordRequest req) {
        ResponsePayload response = authenticationService.resetPassword(req);
        return ResponseEntity.ok(
                response
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponsePayload> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ResponsePayload.builder()
                            .status(400)
                            .message("Refresh token is required")
                            .build()
            );
        }

        try {
            Map<String, String> tokens = refreshTokenService.refreshToken(refreshToken);
            return ResponseEntity.ok(
                    ResponsePayload.builder()
                            .status(200)
                            .message("Token refreshed successfully")
                            .data(tokens)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(
                    ResponsePayload.builder()
                            .status(401)
                            .message(e.getMessage())
                            .build()
            );
        }
    }
}
