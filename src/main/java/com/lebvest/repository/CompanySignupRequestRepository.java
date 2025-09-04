package com.lebvest.repository;

import com.lebvest.model.entities.company.CompanySignupRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanySignupRequestRepository extends JpaRepository<CompanySignupRequest, Long> {
    Optional<CompanySignupRequest> findByEmail(@NotBlank @Email(message = "Email must be of a valid format") String email);
}
