package com.lebvest.service;

import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.*;

@Service
public class CompanyRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(CompanyRegistrationService.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public CompanyRegistrationService(UserRepository userRepository,
                                      CompanyRepository companyRepository,
                                      PasswordEncoder passwordEncoder,
                                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public String registerCompany(CompanyRegistrationRequest req, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessages = new StringBuilder("Validation failed: <br>");
            bindingResult.getAllErrors().forEach(error ->
                    errorMessages.append(error.getDefaultMessage()).append("<br>")
            );
            throw new IllegalArgumentException(errorMessages.toString());
        }

        // Check if user already exists
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("User with this email already exists");
        }

        // Check if company name already exists
        if (companyRepository.findByName(req.getCompanyName()).isPresent()) {
            throw new ConflictException("Company with this name already exists");
        }

        // Create User with COMPANY role
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .roles(Set.of(Role.COMPANY))
                .enabled(true)
                .locked(false)
                .build();
        userRepository.save(user);

        // Handle documents if provided (just store names for now)
        List<String> documentNames = new ArrayList<>();
        if (req.getDocuments() != null && req.getDocuments().length > 0) {
            documentNames = Arrays.stream(req.getDocuments())
                    .map(file -> file.getOriginalFilename())
                    .filter(name -> name != null)
                    .toList();
        }

        // Create Company directly
        Company company = Company.builder()
                .name(req.getCompanyName())
                .description(req.getDescription())
                .location(req.getLocation())
                .foundedYear(req.getFoundedYear())
                .sector(req.getSector())
                .user(user)
                .documents(documentNames)
                .build();
        companyRepository.save(company);

        // Generate JWT token for immediate login
        return jwtService.generateToken(user, "access", user.getId());
    }
}
