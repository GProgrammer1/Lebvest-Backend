package com.lebvest.controller;

import com.lebvest.model.dto.AuthenticationRequest;
import com.lebvest.model.dto.ForgotPasswordRequest;
import com.lebvest.model.dto.ResetPasswordRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.entities.investor.User;
import com.lebvest.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
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
}
