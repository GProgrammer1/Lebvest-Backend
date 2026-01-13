package com.lebvest.service;

import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.InvestmentDto;
import com.lebvest.model.dto.InvestmentStatsDto;
import com.lebvest.model.entities.investment.*;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentStatus;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final InvestorRepository investorRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final UserRepository userRepository;

    public InvestmentService(
            InvestmentRepository investmentRepository,
            InvestorRepository investorRepository,
            InvestorInvestmentRepository investorInvestmentRepository,
            UserRepository userRepository) {
        this.investmentRepository = investmentRepository;
        this.investorRepository = investorRepository;
        this.investorInvestmentRepository = investorInvestmentRepository;
        this.userRepository = userRepository;
    }

    public Page<InvestmentDto> getInvestments(
            InvestmentCategory category,
            RiskLevel riskLevel,
            BigDecimal minReturn,
            Location location,
            CompanySector sector,
            InvestmentType investmentType,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String sort,
            int page,
            int size) {

        // Build sort
        Sort sortObj = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // Query with filters - only show APPROVED investments to public
        Page<Investment> investments = investmentRepository.findInvestmentsWithFilters(
                category, riskLevel, minReturn, location, sector, investmentType, minAmount, maxAmount,
                InvestmentStatus.APPROVED, pageable);

        // Get current user's watchlist if authenticated
        List<Long> watchlistIds = getCurrentUserWatchlistIds();

        return investments.map(inv -> convertToDto(inv, watchlistIds));
    }

    public List<InvestmentDto> getFeaturedInvestments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Investment> investments = investmentRepository.findFeaturedInvestments(pageable);

        List<Long> watchlistIds = getCurrentUserWatchlistIds();
        return investments.stream()
                .map(inv -> convertToDto(inv, watchlistIds))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToWatchlist(Long investmentId) {
        Investor investor = getCurrentInvestor();
        if (investor == null) {
            throw new IllegalStateException("User must be an investor to add to watchlist");
        }

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new IllegalArgumentException("Investment not found"));

        investor.getWatchlist().add(investment);
        investorRepository.save(investor);
    }

    @Transactional
    public void removeFromWatchlist(Long investmentId) {
        Investor investor = getCurrentInvestor();
        if (investor == null) {
            throw new IllegalStateException("User must be an investor to remove from watchlist");
        }

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new IllegalArgumentException("Investment not found"));

        investor.getWatchlist().remove(investment);
        investorRepository.save(investor);
    }

    private Sort buildSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(":");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String field = parts[0].trim();
        String direction = parts[1].trim().toUpperCase();

        Sort.Direction sortDirection = direction.equals("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Map frontend sort options to entity fields
        switch (field.toLowerCase()) {
            case "return":
            case "expectedreturn":
                return Sort.by(sortDirection, "expectedReturn");
            case "amount":
            case "mininvestment":
                return Sort.by(sortDirection, "minInvestment");
            case "deadline":
                return Sort.by(sortDirection, "deadline");
            case "created":
            case "createdat":
                return Sort.by(sortDirection, "createdAt");
            case "featured":
            case "raised":
                return Sort.by(sortDirection, "raisedAmount");
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    // Made protected so CompanyService can use it
    protected InvestmentDto convertToDto(Investment investment, List<Long> watchlistIds) {
        InvestmentDto.InvestmentDtoBuilder builder = InvestmentDto.builder()
                .id(investment.getId())
                .title(investment.getTitle())
                .companyName(investment.getCompany().getName())
                .description(investment.getDescription())
                .category(investment.getCategory())
                .riskLevel(investment.getRiskLevel())
                .expectedReturn(investment.getExpectedReturn())
                .minInvestment(investment.getMinInvestment())
                .targetAmount(investment.getTargetAmount())
                .raisedAmount(investment.getRaisedAmount())
                .location(investment.getLocation())
                .sector(investment.getCompany().getSector() != null ? investment.getCompany().getSector().getValue()
                        : null)
                .investmentType(investment.getInvestmentType())
                .duration(investment.getDurationMonths())
                .imageUrl(investment.getImageUrl())
                .fundingStage(investment.getFundingStage())
                .deadline(investment.getDeadline())
                .createdAt(investment.getCreatedAt())
                .isInWatchlist(watchlistIds != null && watchlistIds.contains(investment.getId()));

        // Convert highlights
        if (investment.getHighlights() != null) {
            builder.highlights(investment.getHighlights().stream()
                    .map(InvestmentHighlight::getHighlight)
                    .collect(Collectors.toList()));
        }

        // Convert AI prediction
        if (investment.getAiPrediction() != null) {
            InvestmentAiPrediction ai = investment.getAiPrediction();
            builder.aiPrediction(InvestmentDto.AiPredictionDto.builder()
                    .profitPrediction(ai.getProfitPrediction())
                    .riskAssessment(ai.getRiskAssessment())
                    .confidenceScore(ai.getConfidenceScore())
                    .build());
        }

        // Convert team members
        if (investment.getTeamMembers() != null) {
            builder.team(investment.getTeamMembers().stream()
                    .map(tm -> InvestmentDto.TeamMemberDto.builder()
                            .name(tm.getName())
                            .role(tm.getRole())
                            .bio(tm.getBio())
                            .imageUrl(tm.getImageUrl())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Convert financials
        if (investment.getFinancials() != null) {
            builder.financials(investment.getFinancials().stream()
                    .map(f -> InvestmentDto.FinancialDto.builder()
                            .revenue(f.getRevenue())
                            .expenses(f.getExpenses())
                            .profit(f.getProfit())
                            .year(f.getYear())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Convert documents
        if (investment.getDocuments() != null) {
            builder.documents(investment.getDocuments().stream()
                    .map(d -> InvestmentDto.DocumentDto.builder()
                            .title(d.getTitle())
                            .type(d.getType())
                            .url(d.getUrl())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Convert updates
        if (investment.getUpdates() != null) {
            builder.updates(investment.getUpdates().stream()
                    .map(u -> InvestmentDto.UpdateDto.builder()
                            .date(u.getUpdateDate())
                            .title(u.getTitle())
                            .content(u.getContent())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    private List<Long> getCurrentUserWatchlistIds() {
        Investor investor = getCurrentInvestor();
        if (investor == null || investor.getWatchlist() == null) {
            return List.of();
        }
        return investor.getWatchlist().stream()
                .map(Investment::getId)
                .collect(Collectors.toList());
    }

    private Investor getCurrentInvestor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> user = userRepository.findByEmail(userDetails.getUsername());
        if (user.isEmpty()) {
            return null;
        }

        return investorRepository.findByUser(user.get()).orElse(null);
    }

    public Page<InvestmentDto> searchInvestments(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Investment> investments = investmentRepository.searchInvestments(query, InvestmentStatus.APPROVED,
                pageable);

        List<Long> watchlistIds = getCurrentUserWatchlistIds();
        return investments.map(inv -> convertToDto(inv, watchlistIds));
    }

    @Transactional(readOnly = true)
    public com.lebvest.model.dto.WatchlistStatusDto getWatchlistStatus(Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        Investor investor = getCurrentInvestor();
        if (investor == null) {
            return com.lebvest.model.dto.WatchlistStatusDto.builder()
                    .isWatchlisted(false)
                    .build();
        }

        boolean isWatchlisted = investor.getWatchlist().contains(investment);

        // If watchlisted, find when it was added (we'd need to track this, for now
        // return null)
        return com.lebvest.model.dto.WatchlistStatusDto.builder()
                .isWatchlisted(isWatchlisted)
                .addedAt(null) // TODO: Track watchlist addition date if needed
                .build();
    }

    @Transactional(readOnly = true)
    public InvestmentStatsDto getInvestmentStats(Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        List<com.lebvest.model.entities.investment.InvestorInvestment> investorInvestments = investorInvestmentRepository
                .findByInvestment(investment);

        Integer totalInvestors = investorInvestments.size();
        BigDecimal targetAmount = investment.getTargetAmount();
        BigDecimal raisedAmount = investment.getRaisedAmount();

        // Calculate progress percentage
        BigDecimal progressPercentage = BigDecimal.ZERO;
        if (targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = raisedAmount
                    .divide(targetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate statistics
        BigDecimal averageInvestmentAmount = BigDecimal.ZERO;
        BigDecimal minInvestmentAmount = BigDecimal.ZERO;
        BigDecimal maxInvestmentAmount = BigDecimal.ZERO;

        if (totalInvestors > 0) {
            BigDecimal totalAmount = investorInvestments.stream()
                    .map(com.lebvest.model.entities.investment.InvestorInvestment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            averageInvestmentAmount = totalAmount
                    .divide(new BigDecimal(totalInvestors), 2, RoundingMode.HALF_UP);

            minInvestmentAmount = investorInvestments.stream()
                    .map(com.lebvest.model.entities.investment.InvestorInvestment::getAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            maxInvestmentAmount = investorInvestments.stream()
                    .map(com.lebvest.model.entities.investment.InvestorInvestment::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        return InvestmentStatsDto.builder()
                .investmentId(investment.getId())
                .investmentTitle(investment.getTitle())
                .targetAmount(targetAmount)
                .raisedAmount(raisedAmount)
                .progressPercentage(progressPercentage)
                .totalInvestors(totalInvestors)
                .averageInvestmentAmount(averageInvestmentAmount)
                .minInvestmentAmount(minInvestmentAmount)
                .maxInvestmentAmount(maxInvestmentAmount)
                .build();
    }

    @Transactional(readOnly = true)
    public InvestmentDto getInvestmentById(Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Only return APPROVED investments for public access
        // Admin endpoints should use AdminService.getProjectForReview instead
        if (investment.getStatus() != InvestmentStatus.APPROVED) {
            throw new ResourceNotFoundException("Investment not found or not available");
        }

        List<Long> watchlistIds = getCurrentUserWatchlistIds();
        return convertToDto(investment, watchlistIds);
    }

    @Transactional
    public InvestorInvestment makeInvestment(Long investmentId, BigDecimal amount) {
        Investor investor = getCurrentInvestor();
        if (investor == null) {
            throw new IllegalStateException("User must be an investor to make an investment");
        }

        if (!investor.getKycVerified()) {
            throw new IllegalStateException("Your account must be verified to make investments.");
        }

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Validate amount
        if (amount.compareTo(investment.getMinInvestment()) < 0) {
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
        BigDecimal newTotalRaised = investment.getRaisedAmount().add(amount);
        if (newTotalRaised.compareTo(investment.getTargetAmount()) > 0) {
            BigDecimal remaining = investment.getTargetAmount().subtract(investment.getRaisedAmount());
            throw new IllegalArgumentException(
                    "Investment amount exceeds remaining target. Maximum investment allowed: " + remaining);
        }

        // Calculate maturity date for this specific investment
        java.time.LocalDate maturityDate = investment.getMaturityDate();
        if (maturityDate == null && investment.getDurationMonths() != null) {
            maturityDate = java.time.LocalDate.now().plusMonths(investment.getDurationMonths());
        }

        // Calculate expected return amount
        BigDecimal expectedReturnAmount = calculateExpectedReturn(amount, investment.getExpectedReturn());

        // Create investor investment
        InvestorInvestment investorInvestment = InvestorInvestment.builder()
                .investor(investor)
                .investment(investment)
                .amount(amount)
                .investedAt(java.time.LocalDate.now())
                .currentValue(amount) // Initially same as invested amount
                .expectedReturnAmount(expectedReturnAmount)
                .maturityDate(maturityDate)
                .isMatured(false)
                .payoutRequested(false)
                .build();

        investorInvestment = investorInvestmentRepository.save(investorInvestment);

        // Update investment raised amount
        BigDecimal newRaisedAmount = investment.getRaisedAmount().add(amount);
        investment.setRaisedAmount(newRaisedAmount);

        // Update funding status
        if (newRaisedAmount.compareTo(investment.getTargetAmount()) >= 0) {
            investment.setFundingStatus(com.lebvest.model.enums.FundingStatus.COMPLETED);
        } else if (investment.getFundingStatus() == com.lebvest.model.enums.FundingStatus.PENDING) {
            investment.setFundingStatus(com.lebvest.model.enums.FundingStatus.PAID);
        }

        // Calculate maturity date if not set (based on durationMonths)
        if (investment.getMaturityDate() == null && investment.getDurationMonths() != null) {
            java.time.LocalDate calculatedMaturityDate = java.time.LocalDate.now()
                    .plusMonths(investment.getDurationMonths());
            investment.setMaturityDate(calculatedMaturityDate);

            // Calculate expected return date (same as maturity for now)
            investment.setExpectedReturnDate(calculatedMaturityDate);
        }
        investmentRepository.save(investment);

        // Update investor totals
        investor.setTotal_invested(investor.getTotal_invested().add(amount));
        investor.setPortfolio_value(investor.getPortfolio_value().add(amount));
        investorRepository.save(investor);

        return investorInvestment;
    }

    /**
     * Calculate expected return amount based on principal and return rate
     */
    private BigDecimal calculateExpectedReturn(BigDecimal principal, BigDecimal expectedReturnRate) {
        // expectedReturnRate is a percentage (e.g., 10.5 for 10.5%)
        BigDecimal rate = expectedReturnRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return principal.multiply(BigDecimal.ONE.add(rate));
    }

    /**
     * Calculate percentage funded for an investment
     */
    public BigDecimal calculatePercentageFunded(Investment investment) {
        if (investment.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return investment.getRaisedAmount()
                .divide(investment.getTargetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @Transactional(readOnly = true)
    public List<InvestmentDto.UpdateDto> getInvestmentUpdates(Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        if (investment.getUpdates() == null) {
            return List.of();
        }

        return investment.getUpdates().stream()
                .sorted((a, b) -> b.getUpdateDate().compareTo(a.getUpdateDate())) // Newest first
                .map(u -> InvestmentDto.UpdateDto.builder()
                        .date(u.getUpdateDate())
                        .title(u.getTitle())
                        .content(u.getContent())
                        .build())
                .collect(Collectors.toList());
    }
}
