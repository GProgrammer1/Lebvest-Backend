package com.lebvest.repository;

import com.lebvest.model.entities.investor.InvestorGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoalRepository extends JpaRepository<InvestorGoal, Long> {
    Optional<InvestorGoal> findByIdAndInvestorId(Long id, Long investorId);
}

