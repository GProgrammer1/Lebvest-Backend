package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanyFinancial;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;
    private final S3Service s3Service;
    private final VarsConfig varsConfig;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Company getAuthenticatedCompany() {
        User user = getAuthenticatedUser();
        return companyRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Company not found for user: " + user.getEmail()));
    }

    public CompanyFinancial addFinancials(CompanyFinancial financial) {
        Company company = getAuthenticatedCompany();
        financial.setCompany(company);
        company.getFinancials().add(financial);
        companyRepo.save(company);
        return financial;
    }

    public String addDocument(MultipartFile file) {
        Company company = getAuthenticatedCompany();
        try {
            String prefix = "companies/" + company.getId() + "/documents";
            String key = s3Service.uploadFile(prefix, file.getOriginalFilename(), file.getInputStream(), file.getSize(), file.getContentType());
            company.getDocuments().add(key);
            companyRepo.save(company);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document", e);
        }
    }
}
