package com.lebvest.service;

import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.investor.InvestorDashboardDto;
import com.lebvest.model.dto.investor.InvestorNotificationDto;
import com.lebvest.model.dto.investor.InvestorPreferenceDto;
import com.lebvest.model.dto.investor.InvestorProfileDto;
import com.lebvest.model.dto.investor.UpdateInvestorPreferenceRequest;
import com.lebvest.model.dto.investor.UpdateInvestorProfileRequest;
import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.CreateGoalRequest;
import com.lebvest.model.dto.UpdateGoalRequest;

import java.math.BigDecimal;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.InvestorGoal;
import com.lebvest.model.entities.investor.InvestorNotification;
import com.lebvest.model.entities.investor.InvestorPreference;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorNotificationRepository;
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
    private final InvestorNotificationRepository investorNotificationRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;

    public InvestorService(InvestorRepository investorRepository,
                           InvestmentRepository investmentRepository,
                           InvestorNotificationRepository investorNotificationRepository,
                           InvestorInvestmentRepository investorInvestmentRepository) {
        this.investorRepository = investorRepository;
        this.investmentRepository = investmentRepository;
        this.investorNotificationRepository = investorNotificationRepository;
        this.investorInvestmentRepository = investorInvestmentRepository;
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

    @Transactional(readOnly = true)
    public InvestorProfileDto getCurrentInvestorProfile() {
        // For profile, we only need basic info, no need to load all relations
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Unable to determine authenticated investor");
        }
        String email = authentication.getName();
        
        // Use a simpler query that only loads user (no collections)
        Investor investor = investorRepository.findByUserEmailForProfile(email)
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found for current user"));
        
        return InvestorProfileDto.builder()
                .id(investor.getId())
                .name(investor.getUser().getName())
                .email(investor.getUser().getEmail())
                .bio(investor.getBio())
                .imageUrl(investor.getImageUrl())
                .portfolioValue(investor.getPortfolio_value())
                .totalInvested(investor.getTotal_invested())
                .totalReturns(investor.getTotal_returns())
                .build();
    }

    @Transactional(readOnly = true)
    public InvestorProfileDto getPublicInvestorProfile(Long id) {
        Investor investor = investorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found"));
        
        return InvestorProfileDto.builder()
                .id(investor.getId())
                .name(investor.getUser().getName())
                .email(investor.getUser().getEmail())
                .bio(investor.getBio())
                .imageUrl(investor.getImageUrl())
                .portfolioValue(investor.getPortfolio_value())
                .totalInvested(investor.getTotal_invested())
                .totalReturns(investor.getTotal_returns())
                .build();
    }

    @Transactional
    public InvestorProfileDto updateCurrentInvestorProfile(UpdateInvestorProfileRequest request) {
        Investor investor = getCurrentInvestorWithRelations();
        
        if (request.getName() != null && !request.getName().isBlank()) {
            investor.getUser().setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            investor.getUser().setEmail(request.getEmail());
        }
        if (request.getBio() != null) {
            investor.setBio(request.getBio());
        }
        if (request.getImageUrl() != null) {
            investor.setImageUrl(request.getImageUrl());
        }
        
        investorRepository.save(investor);
        
        return InvestorProfileDto.builder()
                .id(investor.getId())
                .name(investor.getUser().getName())
                .email(investor.getUser().getEmail())
                .bio(investor.getBio())
                .imageUrl(investor.getImageUrl())
                .portfolioValue(investor.getPortfolio_value())
                .totalInvested(investor.getTotal_invested())
                .totalReturns(investor.getTotal_returns())
                .build();
    }

    @Transactional(readOnly = true)
    public InvestorPreferenceDto getCurrentInvestorPreferences() {
        Investor investor = getCurrentInvestorWithRelations();
        InvestorPreference preferences = investor.getPreferences();
        
        if (preferences == null) {
            return InvestorPreferenceDto.builder()
                    .categories(Set.of())
                    .riskLevels(Set.of())
                    .locations(Set.of())
                    .build();
        }
        
        return InvestorPreferenceDto.builder()
                .categories(toSlugSet(preferences.getCategories()))
                .riskLevels(toSlugSet(preferences.getRiskLevels()))
                .locations(toSlugSet(preferences.getLocations()))
                .build();
    }

    @Transactional
    public InvestorPreferenceDto updateCurrentInvestorPreferences(UpdateInvestorPreferenceRequest request) {
        Investor investor = getCurrentInvestorWithRelations();
        InvestorPreference preferences = investor.getPreferences();
        
        if (preferences == null) {
            preferences = InvestorPreference.builder()
                    .investor(investor)
                    .categories(new HashSet<>(request.getCategories()))
                    .riskLevels(new HashSet<>(request.getRiskLevels()))
                    .locations(new HashSet<>(request.getLocations()))
                    .build();
            investor.setPreferences(preferences);
        } else {
            preferences.setCategories(new HashSet<>(request.getCategories()));
            preferences.setRiskLevels(new HashSet<>(request.getRiskLevels()));
            preferences.setLocations(new HashSet<>(request.getLocations()));
        }
        
        investorRepository.save(investor);
        
        return InvestorPreferenceDto.builder()
                .categories(toSlugSet(preferences.getCategories()))
                .riskLevels(toSlugSet(preferences.getRiskLevels()))
                .locations(toSlugSet(preferences.getLocations()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvestorNotificationDto> getCurrentInvestorNotifications() {
        Investor investor = getCurrentInvestorWithRelations();
        return investorNotificationRepository.findByInvestorOrderByNotifiedAtDesc(investor)
                .stream()
                .map(this::toInvestorNotificationDto)
                .toList();
    }

    @Transactional
    public InvestorNotificationDto markNotificationAsRead(Long notificationId) {
        Investor investor = getCurrentInvestorWithRelations();
        InvestorNotification notification = investorNotificationRepository
                .findByIdAndInvestor(notificationId, investor)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found or does not belong to current investor"));
        
        notification.setRead(true);
        investorNotificationRepository.save(notification);
        
        return toInvestorNotificationDto(notification);
    }

    private InvestorNotificationDto toInvestorNotificationDto(InvestorNotification notification) {
        return InvestorNotificationDto.builder()
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

    @Transactional
    public InvestorDashboardDto.InvestorGoalDto createGoal(CreateGoalRequest request) {
        Investor investor = getCurrentInvestorWithRelations();
        
        InvestorGoal goal = new InvestorGoal(
                investor,
                request.getTitle(),
                request.getTargetAmount(),
                BigDecimal.ZERO, // currentAmount starts at 0
                request.getDeadline()
        );
        
        investor.getGoals().add(goal);
        investorRepository.save(investor);
        
        return toGoalDto(goal);
    }

    @Transactional
    public InvestorDashboardDto.InvestorGoalDto updateGoal(Long goalId, UpdateGoalRequest request) {
        Investor investor = getCurrentInvestorWithRelations();
        
        InvestorGoal goal = investor.getGoals().stream()
                .filter(g -> g.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        
        // Update fields if provided
        if (request.getTitle() != null) {
            goal.setTitle(request.getTitle());
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getDeadline() != null) {
            goal.setDeadline(request.getDeadline());
        }
        // currentAmount is not updated via this endpoint
        
        investorRepository.save(investor);
        
        return toGoalDto(goal);
    }

    @Transactional
    public void deleteGoal(Long goalId) {
        Investor investor = getCurrentInvestorWithRelations();
        
        InvestorGoal goal = investor.getGoals().stream()
                .filter(g -> g.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        
        investor.getGoals().remove(goal);
        investorRepository.save(investor);
    }

    @Transactional(readOnly = true)
    public InvestorDashboardDto.InvestorInvestmentDto getInvestorInvestmentDetails(Long investorInvestmentId) {
        Investor investor = getCurrentInvestorWithRelations();
        
        InvestorInvestment investorInvestment = investorInvestmentRepository.findById(investorInvestmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor investment not found"));
        
        // Verify it belongs to the current investor
        if (!investorInvestment.getInvestor().getId().equals(investor.getId())) {
            throw new IllegalArgumentException("This investment does not belong to the current investor");
        }
        
        return toInvestorInvestmentDto(investorInvestment);
    }
}

