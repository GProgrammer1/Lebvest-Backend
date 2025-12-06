package com.lebvest.controller;

import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.service.StripeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments")
@CrossOrigin("http://localhost:3000")
public class PaymentController {

    private final StripeService stripeService;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/investment-requests/{requestId}/create-payment-intent")
    public ResponseEntity<ResponsePayload> createPaymentIntent(@PathVariable Long requestId) {
        try {
            StripeService.PaymentIntentResponse response = stripeService.createPaymentIntent(requestId);
            return ResponseEntity.ok(
                    ResponsePayload.builder()
                            .status(200)
                            .message("Payment intent created successfully")
                            .data(Map.of(
                                    "clientSecret", response.getClientSecret(),
                                    "paymentIntentId", response.getPaymentIntentId()
                            ))
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create payment intent: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ResponsePayload.builder()
                            .status(400)
                            .message("Failed to create payment intent: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader,
            HttpServletRequest request) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body("Webhook processing failed: " + e.getMessage());
        }
    }
}
