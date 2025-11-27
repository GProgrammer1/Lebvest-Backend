package com.lebvest.repository;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestorInvestmentRepository extends JpaRepository<InvestorInvestment, Long> {
    List<InvestorInvestment> findByInvestment(Investment investment);
    
    @Query("SELECT ii FROM InvestorInvestment ii WHERE ii.investment.id = :investmentId")
    List<InvestorInvestment> findByInvestmentId(@Param("investmentId") Long investmentId);
}

