package com.lebvest.repository;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestmentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentUpdateRepository extends JpaRepository<InvestmentUpdate, Long> {
    List<InvestmentUpdate> findByInvestmentOrderByUpdateDateDesc(Investment investment);
}

