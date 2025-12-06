package com.lebvest.service;

import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.enums.InvestorClassification;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestorKycService {
    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;

    @Transactional
    public Investor updateKycInfo(BigDecimal annualIncome, String riskProfileAssessment) {
        Investor investor = getCurrentInvestor();
        
        investor.setAnnualIncome(annualIncome);
        investor.setRiskProfileAssessment(riskProfileAssessment);
        
        // Auto-classify based on income
        if (annualIncome != null) {
            if (annualIncome.compareTo(BigDecimal.valueOf(200000)) >= 0) {
                investor.setClassification(InvestorClassification.QUALIFIED);
            } else {
                investor.setClassification(InvestorClassification.RETAIL);
            }
        }
        
        return investorRepository.save(investor);
    }

    @Transactional
    public Investor updateKycClassification(Long investorId, InvestorClassification classification, String kycNotes) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investor not found"));
        
        investor.setClassification(classification);
        investor.setKycNotes(kycNotes);
        investor.setKycVerified(classification != InvestorClassification.RETAIL);
        
        return investorRepository.save(investor);
    }

    @Transactional(readOnly = true)
    public Investor getCurrentInvestor() {
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
