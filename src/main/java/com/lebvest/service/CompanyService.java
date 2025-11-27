package com.lebvest.service;

import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.*;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestmentHighlight;
import com.lebvest.model.entities.investment.InvestmentUpdate;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestmentUpdateRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestmentUpdateRepository investmentUpdateRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final UserRepository userRepository;
    private final InvestmentService investmentService;

    public CompanyService(
            CompanyRepository companyRepository,
            InvestmentRepository investmentRepository,
            InvestmentUpdateRepository investmentUpdateRepository,
            InvestorInvestmentRepository investorInvestmentRepository,
            UserRepository userRepository,
            InvestmentService investmentService) {
        this.companyRepository = companyRepository;
        this.investmentRepository = investmentRepository;
        this.investmentUpdateRepository = investmentUpdateRepository;
        this.investorInvestmentRepository = investorInvestmentRepository;
        this.userRepository = userRepository;
        this.investmentService = investmentService;
    }

    @Transactional
    public InvestmentDto createInvestment(CreateInvestmentRequest request) {
        Company company = getCurrentCompany();
        
        Investment investment = Investment.builder()
                .company(company)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .riskLevel(request.getRiskLevel())
                .expectedReturn(request.getExpectedReturn())
                .minInvestment(request.getMinInvestment())
                .targetAmount(request.getTargetAmount())
                .raisedAmount(BigDecimal.ZERO)
                .location(request.getLocation())
                .investmentType(request.getInvestmentType())
                .durationMonths(request.getDurationMonths())
                .deadline(request.getDeadline())
                .imageUrl(request.getImageUrl())
                .fundingStage(request.getFundingStage())
                .build();

        Investment savedInvestment = investmentRepository.save(investment);

        // Add highlights if provided
        if (request.getHighlights() != null && !request.getHighlights().isEmpty()) {
            final Investment finalInvestment = savedInvestment;
            List<InvestmentHighlight> highlights = request.getHighlights().stream()
                    .map(highlight -> InvestmentHighlight.builder()
                            .investment(finalInvestment)
                            .highlight(highlight)
                            .build())
                    .collect(Collectors.toList());
            savedInvestment.setHighlights(highlights);
            savedInvestment = investmentRepository.save(savedInvestment);
        }

        return convertToDto(savedInvestment);
    }

    @Transactional
    public InvestmentDto updateInvestment(Long investmentId, UpdateInvestmentRequest request) {
        Company company = getCurrentCompany();
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Verify ownership
        if (!investment.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("You do not have permission to update this investment");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            investment.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            investment.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            investment.setCategory(request.getCategory());
        }
        if (request.getRiskLevel() != null) {
            investment.setRiskLevel(request.getRiskLevel());
        }
        if (request.getExpectedReturn() != null) {
            investment.setExpectedReturn(request.getExpectedReturn());
        }
        if (request.getMinInvestment() != null) {
            investment.setMinInvestment(request.getMinInvestment());
        }
        if (request.getTargetAmount() != null) {
            investment.setTargetAmount(request.getTargetAmount());
        }
        if (request.getLocation() != null) {
            investment.setLocation(request.getLocation());
        }
        if (request.getInvestmentType() != null) {
            investment.setInvestmentType(request.getInvestmentType());
        }
        if (request.getDurationMonths() != null) {
            investment.setDurationMonths(request.getDurationMonths());
        }
        if (request.getDeadline() != null) {
            investment.setDeadline(request.getDeadline());
        }
        if (request.getImageUrl() != null) {
            investment.setImageUrl(request.getImageUrl());
        }
        if (request.getFundingStage() != null) {
            investment.setFundingStage(request.getFundingStage());
        }

        // Update highlights if provided
        if (request.getHighlights() != null) {
            // Get or initialize the highlights collection
            List<InvestmentHighlight> highlightsList = investment.getHighlights();
            if (highlightsList == null) {
                highlightsList = new ArrayList<>();
                investment.setHighlights(highlightsList);
            }
            
            // Clear existing highlights (this triggers orphan deletion)
            highlightsList.clear();
            
            // Add new highlights to the same collection (don't replace it)
            final List<InvestmentHighlight> finalHighlightsList = highlightsList;
            request.getHighlights().forEach(highlightText -> {
                InvestmentHighlight highlight = InvestmentHighlight.builder()
                        .investment(investment)
                        .highlight(highlightText)
                        .build();
                finalHighlightsList.add(highlight);
            });
        }

        Investment savedInvestment = investmentRepository.save(investment);
        return convertToDto(savedInvestment);
    }

    @Transactional(readOnly = true)
    public InvestmentDetailDto getInvestmentDetail(Long investmentId) {
        Company company = getCurrentCompany();
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Verify ownership
        if (!investment.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("You do not have permission to view this investment");
        }

        // Get investor investments
        List<InvestorInvestment> investorInvestments = investorInvestmentRepository.findByInvestment(investment);

        // Convert to DTOs
        List<InvestmentDetailDto.InvestorInfoDto> investors = investorInvestments.stream()
                .map(ii -> InvestmentDetailDto.InvestorInfoDto.builder()
                        .investorId(ii.getInvestor().getId())
                        .investorName(ii.getInvestor().getUser().getName())
                        .investorEmail(ii.getInvestor().getUser().getEmail())
                        .investedAmount(ii.getAmount())
                        .currentValue(ii.getCurrentValue())
                        .investedAt(ii.getInvestedAt())
                        .build())
                .collect(Collectors.toList());

        InvestmentDto investmentDto = convertToDto(investment);

        return InvestmentDetailDto.builder()
                .investment(investmentDto)
                .investors(investors)
                .build();
    }

    @Transactional
    public InvestmentDto.UpdateDto createInvestmentUpdate(Long investmentId, CreateInvestmentUpdateRequest request) {
        Company company = getCurrentCompany();
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Verify ownership
        if (!investment.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("You do not have permission to create updates for this investment");
        }

        InvestmentUpdate update = new InvestmentUpdate();
        update.setInvestment(investment);
        update.setUpdateDate(request.getUpdateDate());
        update.setTitle(request.getTitle());
        update.setContent(request.getContent());

        update = investmentUpdateRepository.save(update);

        return InvestmentDto.UpdateDto.builder()
                .date(update.getUpdateDate())
                .title(update.getTitle())
                .content(update.getContent())
                .build();
    }

    private Company getCurrentCompany() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Unable to determine authenticated company");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Company company = companyRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not found for current user"));

        return company;
    }

    private InvestmentDto convertToDto(Investment investment) {
        // Use InvestmentService's conversion method
        List<Long> watchlistIds = new ArrayList<>(); // Empty for company view
        return investmentService.convertToDto(investment, watchlistIds);
    }
}

