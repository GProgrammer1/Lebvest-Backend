package com.lebvest.repository;

import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestorRepository extends JpaRepository<Investor, Long> {

    boolean existsByUser(User user);
    
    Optional<Investor> findByUser(User user);

    @EntityGraph(attributePaths = {
            "user",
            "preferences",
            "investments",
            "investments.investment",
            "investments.investment.company",
            "watchlist",
            "watchlist.company",
            "notifications",
            "notifications.relatedInvestment",
            "notifications.relatedInvestment.company",
            "goals"
    })
    Optional<Investor> findOneWithDetailsByUserEmail(String email);
}
