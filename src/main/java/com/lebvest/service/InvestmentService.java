package com.lebvest.service;

import com.lebvest.model.dto.investment.InvestRequest;
import com.lebvest.model.dto.investment.InvestmentDetailResponse;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final InvestorRepository investorRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;

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
    public List<com.lebvest.model.entities.investment.InvestmentDocument> getInvestmentDocuments(Long id) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investment not found"));
        return investment.getDocuments();
    }

    @Transactional(readOnly = true)
    public List<com.lebvest.model.entities.investment.InvestmentUpdate> getInvestmentUpdates(Long id) {
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
