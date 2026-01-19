package com.lebvest.service;

import com.lebvest.controller.CompanyNotificationSseController;
import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.*;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestmentRequest;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.InvestorNotification;
import com.lebvest.model.enums.CompanyNotificationType;
import com.lebvest.model.enums.InvestmentRequestStatus;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestmentRequestRepository;
import com.lebvest.repository.InvestorNotificationRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class InvestmentRequestService {

    private final InvestmentRequestRepository investmentRequestRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestorRepository investorRepository;
    private final InvestorNotificationRepository investorNotificationRepository;
    private final UserRepository userRepository;
    private final IMailService mailService;
    private final CompanyNotificationSseController companyNotificationSseController;

    public InvestmentRequestService(
            InvestmentRequestRepository investmentRequestRepository,
            InvestmentRepository investmentRepository,
            InvestorRepository investorRepository,
            InvestorNotificationRepository investorNotificationRepository,
            UserRepository userRepository,
            IMailService mailService,
            CompanyNotificationSseController companyNotificationSseController) {
        this.investmentRequestRepository = investmentRequestRepository;
        this.investmentRepository = investmentRepository;
        this.investorRepository = investorRepository;
        this.investorNotificationRepository = investorNotificationRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.companyNotificationSseController = companyNotificationSseController;
    }

    @Transactional
    public InvestmentRequest createInvestmentRequest(Long investmentId, CreateInvestmentRequestRequest request) {
        Investor investor = getCurrentInvestor();
        if (investor == null) {
            throw new IllegalStateException("User must be an investor to create an investment request");
        }

        if (!investor.getKycVerified()) {
            throw new IllegalStateException("Your account must be verified to request investments.");
        }

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Validate amount
        if (request.getAmount().compareTo(investment.getMinInvestment()) < 0) {
            throw new IllegalArgumentException("Investment amount must be at least " + investment.getMinInvestment());
        }

        // Check if investment deadline has passed
        if (investment.getDeadline() != null && investment.getDeadline().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Investment deadline has passed");
        }

        // Check if target is already reached
        if (investment.getRaisedAmount().compareTo(investment.getTargetAmount()) >= 0) {
            throw new IllegalArgumentException("Investment target amount has already been reached");
        }

        // Check if this investment would exceed the target
        BigDecimal newTotalRaised = investment.getRaisedAmount().add(request.getAmount());
        if (newTotalRaised.compareTo(investment.getTargetAmount()) > 0) {
            BigDecimal remaining = investment.getTargetAmount().subtract(investment.getRaisedAmount());
            throw new IllegalArgumentException(
                    "Investment amount exceeds remaining target. Maximum investment allowed: " + remaining);
        }

        // Check if there's already a pending request for this investor and investment
        investmentRequestRepository.findByInvestor_IdAndInvestment_IdAndStatus(
                investor.getId(), investmentId, InvestmentRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalStateException("You already have a pending investment request for this project");
                });

        // Create investment request
        InvestmentRequest investmentRequest = InvestmentRequest.builder()
                .investor(investor)
                .investment(investment)
                .amount(request.getAmount())
                .message(request.getMessage())
                .status(InvestmentRequestStatus.PENDING)
                .build();

        investmentRequest = investmentRequestRepository.save(investmentRequest);

        // Send email to company
        sendInvestmentRequestEmailToCompany(investment, investor, investmentRequest);

        // Send SSE notification to company
        String notificationMessage = String.format(
                "Investor %s has requested to invest $%s in your project \"%s\"",
                investor.getUser().getName(),
                request.getAmount(),
                investment.getTitle());
        companyNotificationSseController.notifyCompany(
                investment.getCompany().getId(),
                CompanyNotificationType.INVESTOR_REQUEST,
                "New Investment Request",
                notificationMessage,
                investment.getId());

        log.info("Investment request created: ID={}, Investor={}, Investment={}, Amount={}",
                investmentRequest.getId(), investor.getId(), investmentId, request.getAmount());

        return investmentRequest;
    }

    @Transactional
    public InvestmentRequest acceptInvestmentRequest(Long requestId, AcceptInvestmentRequestRequest acceptRequest) {
        InvestmentRequest investmentRequest = investmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment request not found"));

        if (investmentRequest.getStatus() != InvestmentRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending investment requests can be accepted");
        }

        investmentRequest.setStatus(InvestmentRequestStatus.ACCEPTED);
        investmentRequest.setAcceptedAt(LocalDateTime.now());
        investmentRequest = investmentRequestRepository.save(investmentRequest);

        // Send email to investor
        sendInvestmentAcceptedEmailToInvestor(investmentRequest);

        // Create investor notification
        InvestorNotification notification = new InvestorNotification();
        notification.setInvestor(investmentRequest.getInvestor());
        notification.setInvestorNotificationType(com.lebvest.model.enums.InvestorNotificationType.INVESTMENT_ACCEPTED);
        notification.setTitle("Investment Request Accepted");
        notification.setMessage(String.format(
                "Your investment request of $%s for \"%s\" has been accepted. You can now proceed with payment.",
                investmentRequest.getAmount(),
                investmentRequest.getInvestment().getTitle()));
        notification.setRelatedInvestment(investmentRequest.getInvestment());
        notification.setRead(false);
        investorNotificationRepository.save(notification);

        log.info("Investment request accepted: ID={}", requestId);
        return investmentRequest;
    }

    @Transactional
    public InvestmentRequest rejectInvestmentRequest(Long requestId, RejectInvestmentRequestRequest rejectRequest) {
        InvestmentRequest investmentRequest = investmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment request not found"));

        if (investmentRequest.getStatus() != InvestmentRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending investment requests can be rejected");
        }

        investmentRequest.setStatus(InvestmentRequestStatus.REJECTED);
        investmentRequest.setRejectionReason(rejectRequest.getReason());
        investmentRequest.setRejectedAt(LocalDateTime.now());
        investmentRequest = investmentRequestRepository.save(investmentRequest);

        // Send email to investor
        sendInvestmentRejectedEmailToInvestor(investmentRequest, rejectRequest.getReason());

        log.info("Investment request rejected: ID={}, Reason={}", requestId, rejectRequest.getReason());
        return investmentRequest;
    }

    private Investor getCurrentInvestor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        com.lebvest.model.entities.investor.User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);

        if (user == null) {
            return null;
        }

        return investorRepository.findByUser(user).orElse(null);
    }

    private void sendInvestmentRequestEmailToCompany(Investment investment, Investor investor,
            InvestmentRequest request) {
        try {
            String companyEmail = investment.getCompany().getUser().getEmail();
            String investorName = investor.getUser().getName();
            String projectTitle = investment.getTitle();
            String amount = request.getAmount().toString();

            // TODO: Create email template
            String subject = "New Investment Request - " + projectTitle;
            String htmlContent = String.format(
                    "<h2>New Investment Request</h2>" +
                            "<p>Investor <strong>%s</strong> has requested to invest <strong>$%s</strong> in your project <strong>%s</strong>.</p>"
                            +
                            "<p>Please review the request in your dashboard and accept or reject it.</p>",
                    investorName, amount, projectTitle);

            mailService.sendHtmlMail(companyEmail, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send investment request email to company: {}", e.getMessage(), e);
        }
    }

    private void sendInvestmentAcceptedEmailToInvestor(InvestmentRequest request) {
        try {
            String investorEmail = request.getInvestor().getUser().getEmail();
            String investorName = request.getInvestor().getUser().getName();
            String projectTitle = request.getInvestment().getTitle();
            String amount = request.getAmount().toString();

            String subject = "Investment Request Accepted - " + projectTitle;
            String htmlContent = String.format(
                    "<h2>Investment Request Accepted</h2>" +
                            "<p>Dear %s,</p>" +
                            "<p>Great news! Your investment request of <strong>$%s</strong> for the project <strong>%s</strong> has been accepted.</p>"
                            +
                            "<p>You can now proceed with payment through the secure payment link in your dashboard.</p>",
                    investorName, amount, projectTitle);

            mailService.sendHtmlMail(investorEmail, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send investment accepted email to investor: {}", e.getMessage(), e);
        }
    }

    private void sendInvestmentRejectedEmailToInvestor(InvestmentRequest request, String reason) {
        try {
            String investorEmail = request.getInvestor().getUser().getEmail();
            String investorName = request.getInvestor().getUser().getName();
            String projectTitle = request.getInvestment().getTitle();
            String amount = request.getAmount().toString();

            String subject = "Investment Request Update - " + projectTitle;
            String htmlContent = String.format(
                    "<h2>Investment Request Update</h2>" +
                            "<p>Dear %s,</p>" +
                            "<p>We regret to inform you that your investment request of <strong>$%s</strong> for the project <strong>%s</strong> has been declined.</p>"
                            +
                            "<p><strong>Reason:</strong> %s</p>" +
                            "<p>You can explore other investment opportunities on our platform.</p>",
                    investorName, amount, projectTitle, reason);

            mailService.sendHtmlMail(investorEmail, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send investment rejected email to investor: {}", e.getMessage(), e);
        }
    }
}
