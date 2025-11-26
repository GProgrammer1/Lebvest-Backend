package com.lebvest.repository;

import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestorRepository extends JpaRepository<Investor, Long> {

    boolean existsByUser(User user);
    
    Optional<Investor> findByUser(User user);

    @EntityGraph(attributePaths = {
            "user",
            "preferences",
            "watchlist",
            "watchlist.company",
            "notifications",
            "notifications.relatedInvestment",
            "notifications.relatedInvestment.company"
            // Note: investments and goals are fetched lazily to avoid MultipleBagFetchException
            // They will be loaded when accessed within the @Transactional method
    })
    @Query("SELECT i FROM Investor i WHERE i.user.email = :email")
    Optional<Investor> findOneWithDetailsByUserEmail(@Param("email") String email);
    
    // Simpler method for profile - only loads user, no collections
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT i FROM Investor i WHERE i.user.email = :email")
    Optional<Investor> findByUserEmailForProfile(@Param("email") String email);
}
