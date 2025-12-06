package com.lebvest.service;

import com.lebvest.controller.CompanyNotificationSseController;
import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestmentRequest;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.enums.CompanyNotificationType;
import com.lebvest.model.enums.InvestmentRequestStatus;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestmentRequestRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void logStripeConfig() {
        // Check environment variable directly
        String envVar = System.getenv("STRIPE_SECRET_KEY");
        log.info("Environment variable STRIPE_SECRET_KEY: {}", 
            envVar != null && envVar.length() > 10 ? envVar.substring(0, 10) + "..." : (envVar != null ? envVar : "NOT SET"));
        
        // Log first 10 chars to verify key is loaded (don't log full key for security)
        String keyPreview = stripeSecretKey != null && stripeSecretKey.length() > 10 
            ? stripeSecretKey.substring(0, 10) + "..." 
            : "null or empty";
        log.info("Stripe secret key from @Value: {} (length: {})", 
            keyPreview, 
            stripeSecretKey != null ? stripeSecretKey.length() : 0);
        
        // Check if it's the placeholder
        if (stripeSecretKey != null && (stripeSecretKey.equals("sk_test_...") || stripeSecretKey.contains("..."))) {
            log.error("⚠️  ERROR: Stripe secret key is using placeholder value from application.yml! " +
                    "This means the .env file is NOT being loaded. " +
                    "Please ensure:");
            log.error("1. .env file exists in lebvest-backend/ directory");
            log.error("2. .env file contains: STRIPE_SECRET_KEY=sk_test_YOUR_FULL_KEY (no quotes, no spaces)");
            log.error("3. Restart the Spring Boot application after adding/updating .env");
            log.error("4. Check that spring-dotenv dependency is in pom.xml (it should be)");
        }
    }

    private final InvestmentRequestRepository investmentRequestRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final InvestorRepository investorRepository;
    private final CompanyNotificationSseController companyNotificationSseController;

    public StripeService(
            InvestmentRequestRepository investmentRequestRepository,
            InvestmentRepository investmentRepository,
            InvestorInvestmentRepository investorInvestmentRepository,
            InvestorRepository investorRepository,
            CompanyNotificationSseController companyNotificationSseController) {
        this.investmentRequestRepository = investmentRequestRepository;
        this.investmentRepository = investmentRepository;
        this.investorInvestmentRepository = investorInvestmentRepository;
        this.investorRepository = investorRepository;
        this.companyNotificationSseController = companyNotificationSseController;
    }

    /**
     * Create a Stripe Payment Intent for an accepted investment request
     */
    public PaymentIntentResponse createPaymentIntent(Long requestId) {
        // Validate Stripe key is set and not a placeholder
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty() || 
            stripeSecretKey.equals("sk_test_...") || stripeSecretKey.contains("...")) {
            String keyPreview = stripeSecretKey != null && stripeSecretKey.length() > 10 
                ? stripeSecretKey.substring(0, 10) + "..." 
                : "null or empty";
            log.error("Invalid Stripe secret key configuration. Key value: {}", keyPreview);
            throw new IllegalStateException(
                "Stripe secret key is not properly configured. " +
                "Please set STRIPE_SECRET_KEY in your .env file with a valid Stripe API key from https://dashboard.stripe.com/test/apikeys. " +
                "The key should start with 'sk_test_' and be the FULL key (not 'sk_test_...' placeholder). " +
                "Current value appears to be: " + keyPreview);
        }
        
        // Trim whitespace and validate key format
        stripeSecretKey = stripeSecretKey.trim();
        if (!stripeSecretKey.startsWith("sk_test_") && !stripeSecretKey.startsWith("sk_live_")) {
            log.error("Stripe secret key has invalid format. Must start with 'sk_test_' or 'sk_live_'");
            throw new IllegalStateException("Stripe secret key format is invalid. Must start with 'sk_test_' or 'sk_live_'");
        }
        
        Stripe.apiKey = stripeSecretKey;

        InvestmentRequest request = investmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment request not found"));

        if (request.getStatus() != InvestmentRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted investment requests can be paid");
        }

        if (request.getStripePaymentIntentId() != null) {
            // Payment intent already created, return existing one
            try {
                PaymentIntent existingIntent = PaymentIntent.retrieve(request.getStripePaymentIntentId());
                return PaymentIntentResponse.builder()
                        .clientSecret(existingIntent.getClientSecret())
                        .paymentIntentId(existingIntent.getId())
                        .build();
            } catch (Exception e) {
                log.warn("Failed to retrieve existing payment intent, creating new one: {}", e.getMessage());
            }
        }

        try {
            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("investmentRequestId", requestId.toString());
            metadata.put("investmentId", request.getInvestment().getId().toString());
            metadata.put("investorId", request.getInvestor().getId().toString());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .putAllMetadata(metadata)
                    .setDescription("Investment in " + request.getInvestment().getTitle())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Save payment intent ID to request
            request.setStripePaymentIntentId(paymentIntent.getId());
            investmentRequestRepository.save(request);

            log.info("Created payment intent {} for investment request {}", paymentIntent.getId(), requestId);

            return PaymentIntentResponse.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentIntentId(paymentIntent.getId())
                    .build();

        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe API error creating payment intent: {}", e.getMessage());
            log.error("Stripe error code: {}", e.getCode());
            if (e.getStripeError() != null) {
                log.error("Stripe error details: type={}, param={}", 
                    e.getStripeError().getType(), 
                    e.getStripeError().getParam());
            }
            
            // Provide more helpful error message
            String errorMessage = "Stripe payment error: " + e.getMessage();
            if (e.getCode() != null) {
                errorMessage += " (Code: " + e.getCode() + ")";
            }
            if (e.getMessage() != null && e.getMessage().contains("Invalid API Key")) {
                errorMessage += ". Please verify your STRIPE_SECRET_KEY in .env file is correct and from the same Stripe account. " +
                               "Make sure you're using a TEST mode key (starts with sk_test_) if testing.";
            }
            throw new RuntimeException("Failed to create payment intent: " + errorMessage, e);
        } catch (Exception e) {
            log.error("Failed to create payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    /**
     * Handle Stripe webhook events
     */
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Stripe webhook secret not configured, skipping webhook verification");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature", e);
        }

        log.info("Received Stripe webhook event: {} (ID: {})", event.getType(), event.getId());

        if ("payment_intent.succeeded".equals(event.getType())) {
            handlePaymentSuccess(event);
        } else {
            log.debug("Unhandled webhook event type: {}", event.getType());
        }
    }

    @Transactional
    private void handlePaymentSuccess(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (paymentIntent == null) {
            log.error("Payment intent not found in webhook event");
            return;
        }

        String paymentIntentId = paymentIntent.getId();
        String requestIdStr = paymentIntent.getMetadata().get("investmentRequestId");

        if (requestIdStr == null) {
            log.error("Investment request ID not found in payment intent metadata");
            return;
        }

        Long requestId = Long.parseLong(requestIdStr);
        InvestmentRequest request = investmentRequestRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment request not found for payment intent: " + paymentIntentId));

        if (request.getStatus() == InvestmentRequestStatus.PAID) {
            log.warn("Investment request {} already marked as paid", requestId);
            return;
        }

        // Update request status
        request.setStatus(InvestmentRequestStatus.PAID);
        request.setPaidAt(LocalDateTime.now());
        investmentRequestRepository.save(request);

        // Create InvestorInvestment
        Investment investment = request.getInvestment();
        InvestorInvestment investorInvestment = InvestorInvestment.builder()
                .investor(request.getInvestor())
                .investment(investment)
                .amount(request.getAmount())
                .investedAt(LocalDate.now())
                .currentValue(request.getAmount()) // Initially same as invested amount
                .build();

        investorInvestmentRepository.save(investorInvestment);

        // Update investment raised amount
        BigDecimal newRaisedAmount = investment.getRaisedAmount().add(request.getAmount());
        investment.setRaisedAmount(newRaisedAmount);
        investmentRepository.save(investment);

        // Update investor totals
        var investor = request.getInvestor();
        investor.setTotal_invested(investor.getTotal_invested().add(request.getAmount()));
        investor.setPortfolio_value(investor.getPortfolio_value().add(request.getAmount()));
        investorRepository.save(investor);

        // Notify company about payment received
        String notificationMessage = String.format(
                "Investor %s has completed payment of $%s for your project \"%s\".",
                investor.getUser().getName(),
                request.getAmount(),
                investment.getTitle()
        );
        companyNotificationSseController.notifyCompany(
                investment.getCompany().getId(),
                CompanyNotificationType.FUNDING_MILESTONE,
                "Payment Received",
                notificationMessage,
                investment.getId()
        );

        log.info("Payment successful for investment request {}. Created InvestorInvestment and updated project funding.", requestId);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentIntentResponse {
        private String clientSecret;
        private String paymentIntentId;
    }
}
