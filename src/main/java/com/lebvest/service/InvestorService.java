package com.lebvest.service;

import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.investor.InvestorDashboardDto;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.InvestorGoal;
import com.lebvest.model.entities.investor.InvestorNotification;
import com.lebvest.model.entities.investor.InvestorPreference;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InvestorService {

    private final InvestorRepository investorRepository;
    private final InvestmentRepository investmentRepository;

    public InvestorService(InvestorRepository investorRepository,
                           InvestmentRepository investmentRepository) {
        this.investorRepository = investorRepository;
        this.investmentRepository = investmentRepository;
    }

    @Transactional(readOnly = true)
    public InvestorDashboardDto getCurrentInvestorDashboard() {
        Investor investor = getCurrentInvestorWithRelations();
        var investments = investor.getInvestments()
                .stream()
                .sorted(Comparator.comparing(InvestorInvestment::getInvestedAt).reversed())
                .map(this::toInvestorInvestmentDto)
                .toList();

        var watchlist = investor.getWatchlist()
                .stream()
                .map(this::toInvestmentSummaryDto)
                .toList();

        var notifications = investor.getNotifications()
                .stream()
                .sorted(Comparator.comparing(InvestorNotification::getNotifiedAt).reversed())
                .map(this::toNotificationDto)
                .toList();

        var goals = investor.getGoals()
                .stream()
                .sorted(Comparator.comparing(InvestorGoal::getDeadline))
                .map(this::toGoalDto)
                .toList();

        var recommendations = buildRecommendations(investor, watchlist, investments);

        return InvestorDashboardDto.builder()
                .investor(toInvestorSummary(investor))
                .investments(investments)
                .watchlist(watchlist)
                .notifications(notifications)
                .goals(goals)
                .recommendations(recommendations)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvestorDashboardDto.InvestorInvestmentDto> getCurrentInvestorInvestments() {
        Investor investor = getCurrentInvestorWithRelations();
        return investor.getInvestments()
                .stream()
                .sorted(Comparator.comparing(InvestorInvestment::getInvestedAt).reversed())
                .map(this::toInvestorInvestmentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvestorDashboardDto.InvestmentSummaryDto> getCurrentInvestorWatchlist() {
        Investor investor = getCurrentInvestorWithRelations();
        return investor.getWatchlist()
                .stream()
                .map(this::toInvestmentSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvestorDashboardDto.InvestorGoalDto> getCurrentInvestorGoals() {
        Investor investor = getCurrentInvestorWithRelations();
        return investor.getGoals()
                .stream()
                .sorted(Comparator.comparing(InvestorGoal::getDeadline))
                .map(this::toGoalDto)
                .toList();
    }

    private Investor getCurrentInvestorWithRelations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Unable to determine authenticated investor");
        }
        String email = authentication.getName();
        return investorRepository.findOneWithDetailsByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found for current user"));
    }

    private InvestorDashboardDto.InvestorSummary toInvestorSummary(Investor investor) {
        return InvestorDashboardDto.InvestorSummary.builder()
                .id(investor.getId())
                .name(investor.getUser().getName())
                .email(investor.getUser().getEmail())
                .portfolioValue(investor.getPortfolio_value())
                .totalInvested(investor.getTotal_invested())
                .totalReturns(investor.getTotal_returns())
                .preferences(toPreferencesDto(investor.getPreferences()))
                .build();
    }

    private InvestorDashboardDto.InvestmentPreferencesDto toPreferencesDto(InvestorPreference preferences) {
        if (preferences == null) {
            return InvestorDashboardDto.InvestmentPreferencesDto.builder()
                    .categories(Set.of())
                    .riskLevels(Set.of())
                    .locations(Set.of())
                    .build();
        }
        return InvestorDashboardDto.InvestmentPreferencesDto.builder()
                .categories(toSlugSet(preferences.getCategories()))
                .riskLevels(toSlugSet(preferences.getRiskLevels()))
                .locations(toSlugSet(preferences.getLocations()))
                .build();
    }

    private <T extends Enum<T>> Set<String> toSlugSet(Set<T> enums) {
        if (enums == null || enums.isEmpty()) {
            return Set.of();
        }
        return enums.stream()
                .map(e -> e.name().toLowerCase())
                .collect(Collectors.toSet());
    }

    private InvestorDashboardDto.InvestorInvestmentDto toInvestorInvestmentDto(InvestorInvestment investment) {
        return InvestorDashboardDto.InvestorInvestmentDto.builder()
                .id(investment.getId())
                .amount(investment.getAmount())
                .currentValue(investment.getCurrentValue())
                .investedAt(investment.getInvestedAt())
                .investment(toInvestmentSummaryDto(investment.getInvestment()))
                .build();
    }

    private InvestorDashboardDto.InvestmentSummaryDto toInvestmentSummaryDto(Investment investment) {
        return InvestorDashboardDto.InvestmentSummaryDto.builder()
                .id(investment.getId())
                .title(investment.getTitle())
                .companyName(investment.getCompany().getName())
                .category(toSlug(investment.getCategory()))
                .riskLevel(toSlug(investment.getRiskLevel()))
                .expectedReturn(investment.getExpectedReturn())
                .minInvestment(investment.getMinInvestment())
                .targetAmount(investment.getTargetAmount())
                .raisedAmount(investment.getRaisedAmount())
                .location(toSlug(investment.getLocation()))
                .investmentType(toSlug(investment.getInvestmentType()))
                .durationMonths(investment.getDurationMonths())
                .imageUrl(investment.getImageUrl())
                .fundingStage(investment.getFundingStage())
                .deadline(investment.getDeadline())
                .createdAt(investment.getCreatedAt())
                .build();
    }

    private InvestorDashboardDto.InvestorNotificationDto toNotificationDto(InvestorNotification notification) {
        return InvestorDashboardDto.InvestorNotificationDto.builder()
                .id(notification.getId())
                .type(notification.getInvestorNotificationType() != null
                        ? notification.getInvestorNotificationType().name().toLowerCase()
                        : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notifiedAt(notification.getNotifiedAt())
                .read(notification.isRead())
                .relatedInvestmentId(notification.getRelatedInvestment() != null
                        ? notification.getRelatedInvestment().getId()
                        : null)
                .build();
    }

    private InvestorDashboardDto.InvestorGoalDto toGoalDto(InvestorGoal goal) {
        return InvestorDashboardDto.InvestorGoalDto.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .deadline(goal.getDeadline())
                .build();
    }

    private List<InvestorDashboardDto.InvestmentSummaryDto> buildRecommendations(
            Investor investor,
            List<InvestorDashboardDto.InvestmentSummaryDto> watchlist,
            List<InvestorDashboardDto.InvestorInvestmentDto> investments
    ) {
        Set<Long> excludedIds = new HashSet<>();
        excludedIds.addAll(
                watchlist.stream().map(InvestorDashboardDto.InvestmentSummaryDto::getId).toList()
        );
        excludedIds.addAll(
                investments.stream()
                        .map(dto -> dto.getInvestment().getId())
                        .toList()
        );

        return investmentRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .filter(investment -> !excludedIds.contains(investment.getId()))
                .map(this::toInvestmentSummaryDto)
                .limit(3)
                .toList();
    }

    private String toSlug(InvestmentCategory category) {
        return category != null ? category.name().toLowerCase() : null;
    }

    private String toSlug(RiskLevel riskLevel) {
        return riskLevel != null ? riskLevel.name().toLowerCase() : null;
    }

    private String toSlug(Location location) {
        return location != null ? location.name().toLowerCase() : null;
    }

    private String toSlug(InvestmentType investmentType) {
        return investmentType != null ? investmentType.name().toLowerCase() : null;
    }
}

