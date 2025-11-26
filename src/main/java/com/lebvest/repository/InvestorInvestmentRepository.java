package com.lebvest.repository;

import com.lebvest.model.entities.investment.InvestorInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorInvestmentRepository extends JpaRepository<InvestorInvestment, Long> {
}
