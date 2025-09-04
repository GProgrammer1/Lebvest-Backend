package com.lebvest.service;

import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.InvestorRegistrationRequest;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.InvestorPreference;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class InvestorRegistrationService {

    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public InvestorRegistrationService(InvestorRepository investorRepository,
                                       UserRepository userRepository,
                                       JwtService jwtService,
                                       PasswordEncoder passwordEncoder) {
        this.investorRepository = investorRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String registerInvestor(InvestorRegistrationRequest investorRegistrationRequest) {
        var user = userRepository.findByEmail(investorRegistrationRequest.getEmail()).orElse(null);
        var investorPreferences = InvestorPreference
                .builder()
                .locations(investorRegistrationRequest.getLocations())
                .categories(investorRegistrationRequest.getInvestmentCategories())
                .riskLevels(investorRegistrationRequest.getRiskLevels())
                .build();
        if (user != null) {
            if (investorRepository.existsByUser(user)) {
                throw new ConflictException("Investor profile already exists for this user");
            }
            Set<Role> newRoles = new HashSet<>(user.getRoles());
            newRoles.add(Role.INVESTOR);
            user.setRoles(newRoles);
            userRepository.save(user);
        }  else {
            user = User
                    .builder()
                    .name(investorRegistrationRequest.getName())
                    .email(investorRegistrationRequest.getEmail())
                    .password(passwordEncoder.encode(investorRegistrationRequest.getPassword()))
                    .roles(Set.of(Role.INVESTOR))
                    .build();

            userRepository.save(user);

        }
        var investor = Investor.builder()
                .user(user)
                .bio(investorRegistrationRequest.getBio())
                .preferences(investorPreferences)
                .build();
        investorPreferences.setInvestor(investor);

        investorRepository.save(investor);

        return jwtService.generateToken(user, "access", user.getId());
    }
}
