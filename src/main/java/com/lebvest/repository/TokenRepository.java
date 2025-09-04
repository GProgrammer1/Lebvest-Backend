package com.lebvest.repository;

import com.lebvest.model.entities.ForgotPassToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<ForgotPassToken, Long> {
    Optional<ForgotPassToken> findByToken(String token);
}
