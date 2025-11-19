package com.lebvest.repository;

import com.lebvest.model.entities.investment.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    List<Investment> findTop5ByOrderByCreatedAtDesc();
}

