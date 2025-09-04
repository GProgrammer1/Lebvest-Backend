package com.lebvest.repository;

import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorRepository extends JpaRepository<Investor, Long> {

    boolean existsByUser(User user);
}
