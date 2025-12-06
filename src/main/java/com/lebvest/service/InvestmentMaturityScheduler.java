package com.lebvest.service;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentMaturityScheduler {

    private final InvestmentRepository investmentRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;

    /**
     * Check for matured investments daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkMaturedInvestments() {
        log.info("Starting maturity check for investments");
        LocalDate today = LocalDate.now();

        // Find investments that have reached maturity date
        List<Investment> maturedInvestments = investmentRepository.findAll().stream()
                .filter(inv -> inv.getMaturityDate() != null && 
                              inv.getMaturityDate().isBefore(today) || inv.getMaturityDate().isEqual(today))
                .filter(inv -> !inv.getStatus().equals(com.lebvest.model.enums.InvestmentStatus.REJECTED))
                .toList();

        log.info("Found {} matured investments", maturedInvestments.size());

        for (Investment investment : maturedInvestments) {
            // Mark all investor investments as matured
            List<InvestorInvestment> investorInvestments = investorInvestmentRepository
                    .findByInvestmentId(investment.getId());

            for (InvestorInvestment investorInvestment : investorInvestments) {
                if (!investorInvestment.getIsMatured()) {
                    investorInvestment.setIsMatured(true);
                    
                    // Calculate expected return amount
                    if (investorInvestment.getExpectedReturnAmount() == null) {
                        BigDecimal expectedReturn = calculateExpectedReturn(
                                investorInvestment.getAmount(),
                                investment.getExpectedReturn()
                        );
                        investorInvestment.setExpectedReturnAmount(expectedReturn);
                    }
                    
                    investorInvestmentRepository.save(investorInvestment);
                    log.info("Marked investor investment {} as matured", investorInvestment.getId());
                }
            }
        }

        log.info("Completed maturity check");
    }

    /**
     * Calculate expected return amount based on principal and return rate
     */
    private java.math.BigDecimal calculateExpectedReturn(
            java.math.BigDecimal principal, 
            java.math.BigDecimal expectedReturnRate) {
        // expectedReturnRate is a percentage (e.g., 10.5 for 10.5%)
        java.math.BigDecimal rate = expectedReturnRate.divide(
                java.math.BigDecimal.valueOf(100), 
                4, 
                java.math.RoundingMode.HALF_UP);
        return principal.multiply(java.math.BigDecimal.ONE.add(rate));
    }
}
