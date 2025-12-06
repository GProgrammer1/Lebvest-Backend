package com.lebvest.service;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.entities.investment.PayoutHistory;
import com.lebvest.model.entities.investment.PayoutRequest;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.enums.PayoutStatus;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.PayoutHistoryRepository;
import com.lebvest.repository.PayoutRequestRepository;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {
    private final PayoutRequestRepository payoutRequestRepository;
    private final PayoutHistoryRepository payoutHistoryRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final InvestorRepository investorRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final LocalFileStorageService fileStorageService;

    @Transactional
    public PayoutRequest createPayoutRequest(Long investorInvestmentId) {
        InvestorInvestment investorInvestment = investorInvestmentRepository.findById(investorInvestmentId)
                .orElseThrow(() -> new RuntimeException("Investor investment not found"));

        // Check if already requested
        payoutRequestRepository.findByInvestorInvestment_Id(investorInvestmentId)
                .ifPresent(pr -> {
                    throw new RuntimeException("Payout already requested for this investment");
                });

        Investment investment = investorInvestment.getInvestment();
        Investor investor = investorInvestment.getInvestor();
        Company company = investment.getCompany();

        // Calculate expected return
        BigDecimal expectedReturn = calculateExpectedReturn(
                investorInvestment.getAmount(),
                investment.getExpectedReturn()
        );

        PayoutRequest payoutRequest = PayoutRequest.builder()
                .investor(investor)
                .investment(investment)
                .company(company)
                .investorInvestment(investorInvestment)
                .amount(investorInvestment.getAmount())
                .expectedReturn(expectedReturn)
                .status(PayoutStatus.PENDING)
                .build();

        investorInvestment.setPayoutRequested(true);
        investorInvestmentRepository.save(investorInvestment);

        return payoutRequestRepository.save(payoutRequest);
    }

    @Transactional
    public PayoutRequest submitPayoutEvidence(Long payoutRequestId, MultipartFile evidenceFile) {
        PayoutRequest payoutRequest = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new RuntimeException("Payout request not found"));

        // Verify company owns this payout
        Company currentCompany = getCurrentCompany();
        if (!payoutRequest.getCompany().getId().equals(currentCompany.getId())) {
            throw new RuntimeException("Unauthorized: This payout does not belong to your company");
        }

        // Save evidence file
        String evidenceUrl = fileStorageService.savePayoutEvidence(payoutRequestId, evidenceFile);

        payoutRequest.setPayoutEvidenceUrl(evidenceUrl);
        payoutRequest.setStatus(PayoutStatus.SUBMITTED);
        payoutRequest.setSubmittedAt(LocalDateTime.now());

        return payoutRequestRepository.save(payoutRequest);
    }

    @Transactional
    public PayoutRequest approvePayout(Long payoutRequestId, String adminNotes) {
        PayoutRequest payoutRequest = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new RuntimeException("Payout request not found"));

        if (payoutRequest.getStatus() != PayoutStatus.SUBMITTED) {
            throw new RuntimeException("Payout request must be in SUBMITTED status to approve");
        }

        payoutRequest.setStatus(PayoutStatus.APPROVED);
        payoutRequest.setAdminNotes(adminNotes);
        payoutRequest.setApprovedAt(LocalDateTime.now());

        // Create payout history record
        PayoutHistory payoutHistory = PayoutHistory.builder()
                .investor(payoutRequest.getInvestor())
                .investment(payoutRequest.getInvestment())
                .payoutRequest(payoutRequest)
                .principalAmount(payoutRequest.getAmount())
                .returnAmount(payoutRequest.getExpectedReturn().subtract(payoutRequest.getAmount()))
                .totalPayout(payoutRequest.getExpectedReturn())
                .payoutMethod("MANUAL")
                .completedAt(LocalDateTime.now())
                .build();

        payoutHistoryRepository.save(payoutHistory);

        // Update investor totals
        Investor investor = payoutRequest.getInvestor();
        investor.setTotal_returns(investor.getTotal_returns().add(payoutHistory.getReturnAmount()));
        investorRepository.save(investor);

        // Mark payout as completed
        payoutRequest.setStatus(PayoutStatus.COMPLETED);
        payoutRequest.setCompletedAt(LocalDateTime.now());

        return payoutRequestRepository.save(payoutRequest);
    }

    @Transactional
    public PayoutRequest rejectPayout(Long payoutRequestId, String rejectionReason) {
        PayoutRequest payoutRequest = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new RuntimeException("Payout request not found"));

        payoutRequest.setStatus(PayoutStatus.REJECTED);
        payoutRequest.setRejectionReason(rejectionReason);

        return payoutRequestRepository.save(payoutRequest);
    }

    @Transactional(readOnly = true)
    public List<PayoutRequest> getInvestorPayouts(String status) {
        Investor investor = getCurrentInvestor();
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            PayoutStatus payoutStatus = PayoutStatus.valueOf(status.toUpperCase());
            return payoutRequestRepository.findByInvestor_IdAndStatusOrderByCreatedAtDesc(
                    investor.getId(), payoutStatus);
        }
        return payoutRequestRepository.findByInvestor_IdOrderByCreatedAtDesc(investor.getId());
    }

    @Transactional(readOnly = true)
    public List<PayoutRequest> getCompanyPayouts(String status) {
        Company company = getCurrentCompany();
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            PayoutStatus payoutStatus = PayoutStatus.valueOf(status.toUpperCase());
            return payoutRequestRepository.findByCompany_IdAndStatusOrderByCreatedAtDesc(
                    company.getId(), payoutStatus);
        }
        return payoutRequestRepository.findByCompany_IdOrderByCreatedAtDesc(company.getId());
    }

    @Transactional(readOnly = true)
    public List<PayoutHistory> getInvestorPayoutHistory() {
        Investor investor = getCurrentInvestor();
        return payoutHistoryRepository.findByInvestor_IdOrderByCompletedAtDesc(investor.getId());
    }
    
    @Transactional(readOnly = true)
    public List<PayoutRequest> getAdminPayouts(String status) {
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            PayoutStatus payoutStatus = PayoutStatus.valueOf(status.toUpperCase());
            return payoutRequestRepository.findByStatusOrderByCreatedAtDesc(payoutStatus);
        }
        return payoutRequestRepository.findAll();
    }
    
    /**
     * Convert PayoutRequest entity to DTO
     */
    public com.lebvest.model.dto.PayoutRequestDto convertToDto(PayoutRequest request) {
        return com.lebvest.model.dto.PayoutRequestDto.builder()
                .id(request.getId())
                .investorId(request.getInvestor().getId())
                .investorName(request.getInvestor().getUser().getName())
                .investorEmail(request.getInvestor().getUser().getEmail())
                .investmentId(request.getInvestment().getId())
                .investmentTitle(request.getInvestment().getTitle())
                .companyId(request.getCompany().getId())
                .companyName(request.getCompany().getName())
                .investorInvestmentId(request.getInvestorInvestment().getId())
                .amount(request.getAmount())
                .expectedReturn(request.getExpectedReturn())
                .status(request.getStatus())
                .payoutEvidenceUrl(request.getPayoutEvidenceUrl())
                .adminNotes(request.getAdminNotes())
                .rejectionReason(request.getRejectionReason())
                .stripePayoutId(request.getStripePayoutId())
                .submittedAt(request.getSubmittedAt())
                .approvedAt(request.getApprovedAt())
                .completedAt(request.getCompletedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert PayoutHistory entity to DTO
     */
    public com.lebvest.model.dto.PayoutHistoryDto convertHistoryToDto(PayoutHistory history) {
        return com.lebvest.model.dto.PayoutHistoryDto.builder()
                .id(history.getId())
                .investorId(history.getInvestor().getId())
                .investorName(history.getInvestor().getUser().getName())
                .investmentId(history.getInvestment().getId())
                .investmentTitle(history.getInvestment().getTitle())
                .payoutRequestId(history.getPayoutRequest().getId())
                .principalAmount(history.getPrincipalAmount())
                .returnAmount(history.getReturnAmount())
                .totalPayout(history.getTotalPayout())
                .payoutMethod(history.getPayoutMethod())
                .transactionId(history.getTransactionId())
                .completedAt(history.getCompletedAt())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private BigDecimal calculateExpectedReturn(BigDecimal principal, BigDecimal expectedReturnRate) {
        // expectedReturnRate is a percentage (e.g., 10.5 for 10.5%)
        BigDecimal rate = expectedReturnRate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        return principal.multiply(BigDecimal.ONE.add(rate));
    }

    private Company getCurrentCompany() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new RuntimeException("Not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        com.lebvest.model.entities.investor.User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return companyRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    private Investor getCurrentInvestor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }
        String email = authentication.getName();
        com.lebvest.model.entities.investor.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return investorRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Investor not found"));
    }
}
