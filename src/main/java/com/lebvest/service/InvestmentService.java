package com.lebvest.service;

import com.lebvest.model.dto.InvestmentDto;
import com.lebvest.model.dto.investment.InvestRequest;
import com.lebvest.model.dto.investment.InvestmentDetailResponse;
import com.lebvest.model.entities.investment.*;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;

    // Methods from dev branch
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

        // Query with filters
        Page<Investment> investments = investmentRepository.findInvestmentsWithFilters(
                category, riskLevel, minReturn, location, sector, investmentType, minAmount, maxAmount, pageable
        );

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

    private InvestmentDto convertToDto(Investment investment, List<Long> watchlistIds) {
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
                .sector(investment.getCompany().getSector() != null ? investment.getCompany().getSector().getValue() : null)
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

    // Methods from yahya branch
    @Transactional(readOnly = true)
    public InvestmentDetailResponse getInvestmentDetail(Long id) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investment not found"));

        return InvestmentDetailResponse.builder()
                .id(investment.getId())
                .title(investment.getTitle())
                .description(investment.getDescription())
                .category(investment.getCategory())
                .riskLevel(investment.getRiskLevel())
                .expectedReturn(investment.getExpectedReturn())
                .minInvestment(investment.getMinInvestment())
                .targetAmount(investment.getTargetAmount())
                .raisedAmount(investment.getRaisedAmount())
                .location(investment.getLocation())
                .investmentType(investment.getInvestmentType())
                .durationMonths(investment.getDurationMonths())
                .imageUrl(investment.getImageUrl())
                .fundingStage(investment.getFundingStage())
                .deadline(investment.getDeadline())
                .aiPrediction(investment.getAiPrediction())
                .highlights(investment.getHighlights())
                .teamMembers(investment.getTeamMembers())
                .financials(investment.getFinancials())
                .documents(investment.getDocuments())
                .updates(investment.getUpdates())
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvestmentDocument> getInvestmentDocuments(Long id) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investment not found"));
        return investment.getDocuments();
    }

    @Transactional(readOnly = true)
    public List<InvestmentUpdate> getInvestmentUpdates(Long id) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investment not found"));
        return investment.getUpdates();
    }

    @Transactional
    public void invest(Long id, InvestRequest request, String investorEmail) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investment not found"));

        Investor investor = investorRepository.findOneWithDetailsByUserEmail(investorEmail)
                .orElseThrow(() -> new RuntimeException("Investor not found"));

        if (request.getAmount().compareTo(investment.getMinInvestment()) < 0) {
            throw new RuntimeException("Investment amount is less than minimum required");
        }

        InvestorInvestment investorInvestment = InvestorInvestment.builder()
                .investor(investor)
                .investment(investment)
                .amount(request.getAmount())
                .investedAt(LocalDate.now())
                .currentValue(request.getAmount()) // Initial value same as invested amount
                .build();

        investorInvestmentRepository.save(investorInvestment);

        // Update raised amount
        investment.setRaisedAmount(investment.getRaisedAmount().add(request.getAmount()));
        investmentRepository.save(investment);
    }
}
